package org.openapifactory.typescript.files;

import org.openapifactory.api.FileGenerator;
import org.openapifactory.api.codegen.types.CodegenAllOfModel;
import org.openapifactory.api.codegen.types.CodegenArrayModel;
import org.openapifactory.api.codegen.types.CodegenArrayType;
import org.openapifactory.api.codegen.types.CodegenEnum;
import org.openapifactory.api.codegen.types.CodegenEnumModel;
import org.openapifactory.api.codegen.types.CodegenGenericModel;
import org.openapifactory.api.codegen.types.CodegenEmbeddedEnumType;
import org.openapifactory.api.codegen.types.CodegenModel;
import org.openapifactory.api.codegen.types.CodegenOneOfModel;
import org.openapifactory.api.codegen.CodegenProperty;
import org.openapifactory.api.codegen.types.CodegenTypeRef;
import org.openapifactory.api.codegen.OpenapiSpec;
import org.openapifactory.typescript.TypescriptFragments;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;

import static org.openapifactory.api.StringUtil.indent;
import static org.openapifactory.api.StringUtil.join;
import static org.openapifactory.typescript.TypescriptFragments.docString;
import static org.openapifactory.typescript.TypescriptFragments.getRequestTypeName;
import static org.openapifactory.typescript.TypescriptFragments.getResponseTypeName;
import static org.openapifactory.typescript.TypescriptFragments.getTypeName;
import static org.openapifactory.typescript.TypescriptFragments.propertyDefinition;

public class ModelTsFile implements FileGenerator {
    private final OpenapiSpec spec;

    public ModelTsFile(OpenapiSpec spec) {
        this.spec = spec;
    }

    @Override
    public void generate(Path outputRoot) throws IOException {
        Files.writeString(outputRoot.resolve("model.ts"), content());
    }

    private String content() {
        var sections = new ArrayList<String>();
        sections.add(TypescriptFragments.documentationSection(spec));
        for (var model : spec.getModels()) {
            sections.add(modelSection(model));
        }
        return String.join("", sections);
    }

    private String modelSection(CodegenModel model) {
        if (model instanceof CodegenEnumModel enumModel) {
            return enumDeclaration(enumModel);
        } else if (model instanceof CodegenGenericModel generic) {
            return modelGenericSection(generic);
        } else if (model instanceof CodegenAllOfModel allOf) {
            return modelAllOfSection(allOf);
        } else if (model instanceof CodegenOneOfModel oneOf) {
            return modelOneOfSection(oneOf);
        } else if (model instanceof CodegenArrayModel array) {
            return "";
        } else {
            throw new IllegalArgumentException("Unsupported " + model);
        }
    }

    private static String enumDeclaration(CodegenEnum enumType) {
        var name = getTypeName(enumType);
        return "export const " + name + "Values = [\n" +
               indent(4, enumType.getValues(),
                       s -> enumType.isString() ? "\"" + s + "\",\n" : s + ",\n") +
               "] as const;\n" +
               "\n" +
               docString(enumType.getDescription()) +
               "export type " + name + " = typeof " + name + "Values[number];\n";
    }

    protected String modelGenericSection(CodegenGenericModel generic) {
        var result = "\n" + docString(generic.getDescription()) +
                     "export interface " + getTypeName(generic) + " {\n" +
                     join(generic.getAllProperties(), this::modelPropertyDefinition) +
                     "}\n";
        result += readOnlySection(generic);
        result += writeOnlySection(generic);
        result += inlineEnumSection(generic.getAllProperties());
        return result;
    }

    private String readOnlySection(CodegenModel model) {
        if (!model.hasReadOnlyProperties()) {
            return "";
        }
        if (model instanceof CodegenGenericModel generic) {
            return "\nexport type " + getRequestTypeName(model) + " = " +
                   "Omit<" + getTypeName(model) + ", " +
                   join("|", generic.getOmittedPropertiesForReadOnly(), p -> "\"" + p.getName() + "\"") + ">" +
                   join(generic.getReferencesWithReadOnlyProperties(), p -> "\n    & { " + p.getName() + ": " + getRequestTypeName(p.getType()) + " }") +
                   ";\n";
        } else if (model instanceof CodegenOneOfModel oneOf) {
            return "\nexport type " + getRequestTypeName(model) + " = " + join(" | ", oneOf.getOneOf(), TypescriptFragments::getRequestTypeName)  + "\n";
        } else if (model instanceof CodegenAllOfModel allOf) {
            return "\nexport type " + getRequestTypeName(model) + " = " +
                   "Omit<" + getTypeName(model) + ", " +
                   join("|", allOf.getOmittedPropertiesForReadOnly(), p -> "\"" + p.getName() + "\"") + ">" +
                   join(allOf.getReferencesWithReadOnlyProperties(), p -> "\n    & { " + p.getName() + ": " + getRequestTypeName(p.getType()) + " }") +
                   ";\n";
        } else {
            throw new IllegalArgumentException(model.toString());
        }
    }

    private String writeOnlySection(CodegenModel model) {
        if (!model.hasWriteOnlyProperties()) {
            return "";
        }
        if (model instanceof CodegenGenericModel generic) {
            return "\nexport type " + getResponseTypeName(model) + " = " +
                   "Omit<" + getTypeName(model) + ", " +
                   join("|", generic.getOmittedPropertiesForWriteOnly(), p -> "\"" + p.getName() + "\"") + ">" +
                   join(generic.getReferencesWithWriteOnlyProperties(), p -> "\n    & { " + p.getName() + ": " + getResponseTypeName(p.getType()) + " }") +
                   ";\n";
        } else if (model instanceof CodegenOneOfModel oneOf) {
            return "\nexport type " + getResponseTypeName(model) + " = " + "SOMETHING;";
        } else if (model instanceof CodegenAllOfModel allOf) {
            return "\nexport type " + getResponseTypeName(model) + " = " +
                   "Omit<" + getTypeName(model) + ", " +
                   join("|", allOf.getOmittedPropertiesForReadOnly(), p -> "\"" + p.getName() + "\"") + ">" +
                   join(allOf.getReferencesWithReadOnlyProperties(), p -> "\n    & { " + p.getName() + ": " + getResponseTypeName(p.getType()) + " }") +
                   ";\n";
        } else {
            throw new IllegalArgumentException(model.toString());
        }
    }

    private static String inlineEnumSection(Collection<CodegenProperty> properties) {
        var result = "";
        for (var property : properties) {
            if (property.getType() instanceof CodegenEmbeddedEnumType enumType) {
                result += "\n" + enumDeclaration(enumType);
            } else if (property.getType() instanceof CodegenArrayType arrayType) {
                if (arrayType.getItems() instanceof CodegenEmbeddedEnumType enumType) {
                    result += "\n" + enumDeclaration(enumType);
                }
            }
        }
        return result;
    }

    private String modelAllOfSection(CodegenAllOfModel allOf) {
        if (allOf.getInlineSuperModels().isEmpty()) {
            return "\n" +
                   "export type " + getTypeName(allOf) + " = " +
                   join(" & ", allOf.getRefSuperModels(), TypescriptFragments::getTypeName) +
                   ";\n" + readOnlySection(allOf) + inlineEnumSection(allOf.getOwnProperties());
        } else if (allOf.getRefSuperModels().size() == 1) {
            var superClass = (CodegenTypeRef) allOf.getRefSuperModels().get(0);
            return "\n" +
                   "export interface " + getTypeName(allOf) + " extends " + getTypeName(superClass) + " {\n" +
                   indent(4, allOf.getOwnProperties(), p -> propertyDefinition(p) + ";\n") +
                   "}\n" + readOnlySection(allOf) + inlineEnumSection(allOf.getOwnProperties());
        } else {
            return "\n" +
                   "export type " + getTypeName(allOf) + " = " +
                   join(" & ", allOf.getRefSuperModels(), TypescriptFragments::getTypeName) +
                   " & {\n" +
                   indent(4, allOf.getOwnProperties(), p -> propertyDefinition(p) + ";\n") +
                   "};\n" +
                   readOnlySection(allOf) +
                   inlineEnumSection(allOf.getOwnProperties());
        }
    }

    private String modelOneOfSection(CodegenOneOfModel oneOf) {
        var typeName = getTypeName(oneOf);
        var discriminator = oneOf.getDiscriminator();

        if (discriminator.getPropertyName() == null) {
            return "\n" +
                   "export type " + typeName + " = " +
                   join(" | ", oneOf.getOneOf(), TypescriptFragments::getTypeName) +
                   ";\n" + readOnlySection(oneOf);
        }

        return "\n" +
               "export type " + typeName + " =\n" +
               join(" |\n", oneOf.getMappedModels(), mapped ->
                       "    " + ((mapped.getType() instanceof CodegenOneOfModel)
                               ? "" : "{ " + discriminator.getPropertyName() + ": \"" + mapped.getName() + "\" } & ") + getTypeName(mapped.getType())
               ) + ";\n" +
               readOnlySection(oneOf) +
               "\n" +
               "export const " + typeName + "Discriminators = [\n" +
               indent(4, oneOf.getMappedModels(), s -> {
                   if (s.getType() instanceof CodegenOneOfModel subOneOf) {
                       return join(subOneOf.getMappedModels(), s2 -> "\"" + s2.getName() + "\",\n");
                   } else {
                       return "\"" + s.getName() + "\",\n";
                   }
               }) +
               "] as const;\n" +
               "\n" +
               "export type " + typeName + "Discriminator = typeof " + typeName + "Discriminators[number];\n";
    }

    protected String modelPropertyDefinition(CodegenProperty p) {
        return (docString(p.getDescription()) + propertyDefinition(p) + ";").indent(4);
    }

}

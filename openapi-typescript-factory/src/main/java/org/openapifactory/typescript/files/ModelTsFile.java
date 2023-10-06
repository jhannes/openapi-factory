package org.openapifactory.typescript.files;

import org.openapifactory.api.FileGenerator;
import org.openapifactory.api.codegen.CodegenAllOfModel;
import org.openapifactory.api.codegen.CodegenArrayType;
import org.openapifactory.api.codegen.CodegenEnum;
import org.openapifactory.api.codegen.CodegenEnumModel;
import org.openapifactory.api.codegen.CodegenGenericModel;
import org.openapifactory.api.codegen.CodegenInlineEnumType;
import org.openapifactory.api.codegen.CodegenModel;
import org.openapifactory.api.codegen.CodegenOneOfModel;
import org.openapifactory.api.codegen.CodegenProperty;
import org.openapifactory.api.codegen.CodegenTypeRef;
import org.openapifactory.api.codegen.OpenapiSpec;
import org.openapifactory.typescript.TypescriptFragments;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;

import static org.openapifactory.api.StringUtil.indent;
import static org.openapifactory.api.StringUtil.join;
import static org.openapifactory.typescript.TypescriptFragments.getRequestTypeName;
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
        } else {
            throw new IllegalArgumentException("Unsupported " + model);
        }
    }

    private static String enumDeclaration(CodegenEnum enumType) {
        var name = enumType.getName();
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
                     "export interface " + generic.getName() + " {\n" +
                     join(generic.getAllProperties(), this::modelPropertyDefinition) +
                     "}\n";
        result += readOnlySection(generic);
        result += inlineEnumSection(generic.getAllProperties());
        return result;
    }

    private String readOnlySection(CodegenModel model) {
        if (!model.hasReadOnlyProperties()) {
            return "";
        }
        if (model instanceof CodegenGenericModel generic) {
            return "\nexport type " + generic.getName() + "Request = " +
                   "Omit<" + generic.getName() + ", " +
                   join("|", generic.getOmittedPropertiesForReadOnly(), p -> "\"" + p.getName() + "\"") + ">" +
                   join(generic.getReferencesWithReadOnlyProperties(), p -> "\n    & { " + p.getName() + ": " + getRequestTypeName(p.getType()) + " }") +
                   ";\n";
        } else if (model instanceof CodegenOneOfModel oneOf) {
            return "\nexport type " + oneOf.getName() + "Request = " + "SOMETHING;";
        } else if (model instanceof CodegenAllOfModel allOf) {
            return "\nexport type " + allOf.getName() + "Request = " +
                   "Omit<" + allOf.getName() + ", " +
                   join("|", allOf.getOmittedPropertiesForReadOnly(), p -> "\"" + p.getName() + "\"") + ">" +
                   join(allOf.getReferencesWithReadOnlyProperties(), p -> "\n    & { " + p.getName() + ": " + getRequestTypeName(p.getType()) + " }") +
                   ";\n";
        } else {
            throw new IllegalArgumentException(model.toString());
        }
    }

    private static String inlineEnumSection(Collection<CodegenProperty> properties) {
        var result = "";
        for (var property : properties) {
            if (property.getType() instanceof CodegenInlineEnumType enumType) {
                result += "\n" + enumDeclaration(enumType);
            } else if (property.getType() instanceof CodegenArrayType arrayType) {
                if (arrayType.getItems() instanceof CodegenInlineEnumType enumType) {
                    result += "\n" + enumDeclaration(enumType);
                }
            }
        }
        return result;
    }

    private String modelAllOfSection(CodegenAllOfModel allOf) {
        if (allOf.getInlineSuperModels().isEmpty()) {
            return "\n" +
                   "export type " + allOf.getName() + " = " +
                   join(" & ", allOf.getRefSuperModels(), m -> m.getClassName() + "Dto") +
                   ";\n" + readOnlySection(allOf) + inlineEnumSection(allOf.getOwnProperties());
        } else if (allOf.getRefSuperModels().size() == 1) {
            var superClass = (CodegenTypeRef) allOf.getRefSuperModels().get(0);
            return "\n" +
                   "export interface " + allOf.getName() + " extends " + superClass.getClassName() + "Dto {\n" +
                   indent(4, allOf.getOwnProperties(), p -> propertyDefinition(p) + ";\n") +
                   "}\n" + readOnlySection(allOf) + inlineEnumSection(allOf.getOwnProperties());
        } else {
            return "\n" +
                   "export type " + allOf.getName() + " = " +
                   join(" & ", allOf.getRefSuperModels(), m -> m.getClassName() + "Dto") +
                   " & {\n" +
                   indent(4, allOf.getOwnProperties(), p -> propertyDefinition(p) + ";\n") +
                   "};\n" +
                   readOnlySection(allOf) +
                   inlineEnumSection(allOf.getOwnProperties());
        }
    }

    private String modelOneOfSection(CodegenOneOfModel oneOf) {
        var typeName = oneOf.getName();
        var discriminator = oneOf.getDiscriminator();

        if (discriminator.getPropertyName() == null) {
            return "\n" +
                   "export type " + typeName + " = " +
                   join(" | ", oneOf.getOneOf(), TypescriptFragments::getTypeName) +
                   ";\n";
        }

        return "\n" +
               "export type " + typeName + " =\n" +
               join(" |\n", oneOf.getMappedModels(), mapped ->
                       "    " + ((mapped.getType() instanceof CodegenOneOfModel)
                               ? "" : "{ " + discriminator.getPropertyName() + ": \"" + mapped.getName() + "\" } & ") + getTypeName(mapped.getType())
               ) + ";\n" +
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

    private static String docString(String description) {
        if (description == null) return "";
        return "/**\n * " + description + "\n */\n";
    }

}

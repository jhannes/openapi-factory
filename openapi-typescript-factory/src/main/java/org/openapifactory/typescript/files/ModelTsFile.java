package org.openapifactory.typescript.files;

import org.openapifactory.api.StringUtil;
import org.openapifactory.api.codegen.CodegenArrayType;
import org.openapifactory.api.codegen.CodegenEnumModel;
import org.openapifactory.api.codegen.CodegenGenericModel;
import org.openapifactory.api.codegen.CodegenInlineEnumType;
import org.openapifactory.api.codegen.CodegenModel;
import org.openapifactory.api.FileGenerator;
import org.openapifactory.api.codegen.CodegenOneOfModel;
import org.openapifactory.api.codegen.CodegenProperty;
import org.openapifactory.api.codegen.OpenapiSpec;
import org.openapifactory.typescript.TypescriptFragments;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.TreeSet;

import static org.openapifactory.api.StringUtil.indent;
import static org.openapifactory.api.StringUtil.join;
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
            return modelEnumSection(enumModel);
        } else if (model instanceof CodegenGenericModel generic) {
            return modelGenericSection(generic);
        } else if (model instanceof CodegenOneOfModel oneOf) {
            return modelOneOfSection(oneOf);
        } else {
            throw new IllegalArgumentException("Unsupported " + model);
        }
    }

    private static String modelEnumSection(CodegenEnumModel enumModel) {
        var values = StringUtil.lines(enumModel.getValues(), s -> "    \"" + s + "\",");
        return """
                export const %sValues = [
                %s
                ] as const;
                                
                export type %s = typeof %sValues[number];
                """.formatted(
                        enumModel.getName(),
                        values,
                        enumModel.getName(),
                        enumModel.getName());
    }

    protected String modelGenericSection(CodegenGenericModel generic) {
        var result = "\n" + docString(generic.getDescription()) +
                "export interface " + generic.getName() + " {\n" +
                join(generic.getProperties().values(), this::modelPropertyDefinition) +
                "}\n";
        for (var property : generic.getProperties().values()) {
            if (property.getType() instanceof CodegenInlineEnumType enumType) {
                result += "\nexport const " + getTypeName(enumType) + "Values = [\n" +
                          join(enumType.getValues(), s -> ("\"" + s + "\",").indent(4)) +
                          "] as const;\n\n";
                result += "export type " + getTypeName(enumType) + " = typeof " + getTypeName(enumType) + "Values[number];\n";
            } else if (property.getType() instanceof CodegenArrayType arrayType) {
                if (arrayType.getItems() instanceof CodegenInlineEnumType enumType) {
                    result += "\nexport const " + getTypeName(enumType) + "Values = [\n" +
                              join(enumType.getValues(), s -> ("\"" + s + "\",").indent(4)) +
                              "] as const;\n\n";
                    result += "export type " + getTypeName(enumType) + " = typeof " + getTypeName(enumType) + "Values[number];\n";
                }
            }
        }

        return result;
    }

    private String modelOneOfSection(CodegenOneOfModel oneOf) {
        var typeName = oneOf.getName();
        var discriminator = oneOf.getDiscriminator();
        var mapping = discriminator.getMapping();
        return "\n" +
               "export type " + typeName + " =\n" +
               join(" |\n", new TreeSet<>(mapping.keySet()), key ->
                       "    { " + discriminator.getPropertyName() + ": \"" + key + "\" } & " + getTypeName(mapping.get(key)) + ""
               ) + ";\n" +
               "\n" +
               "export const " + typeName + "Discriminators = [\n" +
               indent(4, new TreeSet<>(mapping.keySet()), s -> "\"" + s + "\",\n") +
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

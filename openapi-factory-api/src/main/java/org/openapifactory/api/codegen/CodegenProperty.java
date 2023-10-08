package org.openapifactory.api.codegen;

import lombok.Data;
import lombok.SneakyThrows;
import lombok.ToString;
import org.openapifactory.api.codegen.types.CodegenArrayModel;
import org.openapifactory.api.codegen.types.CodegenSchema;

@Data
public class CodegenProperty implements CodegenProp, Cloneable {
    @ToString.Exclude
    private final OpenapiSpec spec;
    @ToString.Exclude
    private final CodegenObjectSchema model;
    private final String name;
    @ToString.Exclude
    private CodegenSchema schema;
    private String description, example;
    private boolean required, readOnly, writeOnly, nullable;

    @ToString.Exclude
    private CodegenXml xml;

    @Override
    public String getDescription() {
        if (description != null) {
            return description;
        } else if (schema.getReferencedType() instanceof CodegenArrayModel array) {
            return array.getDescription();
        }
        return null;
    }

    @SneakyThrows
    @Override
    public CodegenProperty clone() {
        return (CodegenProperty) super.clone();
    }
}

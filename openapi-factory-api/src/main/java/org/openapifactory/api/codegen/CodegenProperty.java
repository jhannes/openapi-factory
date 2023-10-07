package org.openapifactory.api.codegen;

import lombok.Data;
import lombok.SneakyThrows;
import lombok.ToString;
import org.openapifactory.api.codegen.types.CodegenArrayModel;
import org.openapifactory.api.codegen.types.CodegenType;

@Data
public class CodegenProperty implements CodegenProp, Cloneable {
    @ToString.Exclude
    private final OpenapiSpec spec;
    @ToString.Exclude
    private final CodegenPropertyModel model;
    private final String name;
    @ToString.Exclude
    private CodegenType type;
    private String description, example;
    private boolean required, readOnly, writeOnly, nullable;


    @Override
    public String getDescription() {
        if (description != null) {
            return description;
        } else if (type.getReferencedType() instanceof CodegenArrayModel array) {
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

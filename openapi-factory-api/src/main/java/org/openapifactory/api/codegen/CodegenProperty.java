package org.openapifactory.api.codegen;

import lombok.Data;
import lombok.SneakyThrows;
import lombok.ToString;
import org.openapifactory.api.codegen.types.CodegenType;

@Data
public class CodegenProperty implements CodegenProp, Cloneable {
    @ToString.Exclude
    private final OpenapiSpec spec;
    @ToString.Exclude
    private final CodegenPropertyModel model;
    private final String name;
    private String description, example;
    private boolean required, readOnly, writeOnly, nullable;

    @ToString.Exclude
    private CodegenType type;

    @SneakyThrows
    @Override
    public CodegenProperty clone() {
        return (CodegenProperty) super.clone();
    }
}

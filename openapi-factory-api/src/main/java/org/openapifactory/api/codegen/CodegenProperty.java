package org.openapifactory.api.codegen;

import lombok.Data;
import lombok.SneakyThrows;
import lombok.ToString;

@Data
public class CodegenProperty implements CodegenProp, Cloneable {
    @ToString.Exclude
    private final OpenapiSpec spec;
    private final String name;
    private String description, example;
    private boolean required, readOnly, writeOnly;

    @ToString.Exclude
    private CodegenType type;

    @SneakyThrows
    @Override
    protected CodegenProperty clone() {
        return (CodegenProperty) super.clone();
    }
}

package org.openapifactory.api.codegen;

import lombok.Data;
import lombok.SneakyThrows;
import lombok.ToString;

@Data
public class CodegenProperty implements CodegenProp, Cloneable {
    private final String name;
    private String description, example;
    private boolean required;

    @ToString.Exclude
    private CodegenType type;

    @SneakyThrows
    @Override
    protected CodegenProperty clone() {
        return (CodegenProperty) super.clone();
    }
}

package org.openapifactory.api.codegen;

import lombok.Data;

@Data
public class CodegenProperty implements CodegenProp {
    private final String name;
    private String description, example;
    private boolean required;
    private CodegenType type;
}

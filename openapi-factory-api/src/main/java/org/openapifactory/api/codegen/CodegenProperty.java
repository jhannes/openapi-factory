package org.openapifactory.api.codegen;

import lombok.Data;

@Data
public class CodegenProperty implements CodegenProp {
    private String name;
    private String description, example;
    private boolean required;
    private CodegenType type;
}

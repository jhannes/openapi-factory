package org.openapifactory.api.codegen.types;

import lombok.Data;

@Data
public class CodegenConstantType implements CodegenType {
    private final String value;
}

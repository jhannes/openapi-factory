package org.openapifactory.api.codegen;

import lombok.Data;

@Data
public class CodegenPrimitiveType implements CodegenType {
    private String type, format;
}

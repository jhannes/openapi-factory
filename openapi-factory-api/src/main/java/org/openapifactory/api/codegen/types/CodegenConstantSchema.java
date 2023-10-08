package org.openapifactory.api.codegen.types;

import lombok.Data;

@Data
public class CodegenConstantSchema implements CodegenSchema {
    private final String value;
}

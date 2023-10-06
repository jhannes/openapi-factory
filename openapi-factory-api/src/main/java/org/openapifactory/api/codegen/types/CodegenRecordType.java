package org.openapifactory.api.codegen.types;

import lombok.Data;

@Data
public class CodegenRecordType implements CodegenType {
    private CodegenType additionalProperties;
}

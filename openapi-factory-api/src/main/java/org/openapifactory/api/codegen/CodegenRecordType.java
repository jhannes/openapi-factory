package org.openapifactory.api.codegen;

import lombok.Data;

@Data
public class CodegenRecordType implements CodegenType {
    private CodegenType additionalProperties;
}

package org.openapifactory.api.codegen.types;

import lombok.Data;

@Data
public class CodegenRecordSchema implements CodegenSchema {
    private CodegenSchema additionalProperties;
}

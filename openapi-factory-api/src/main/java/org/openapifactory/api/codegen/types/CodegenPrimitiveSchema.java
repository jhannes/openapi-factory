package org.openapifactory.api.codegen.types;

import lombok.Data;

@Data
public class CodegenPrimitiveSchema implements CodegenSchema {
    private String type, format;

    @Override
    public boolean isDate() {
        return type.equals("string") && ("date".equals(format) || "date-time".equals(format));
    }
}

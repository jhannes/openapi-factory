package org.openapifactory.api.codegen.types;

import lombok.Data;

@Data
public class CodegenPrimitiveType implements CodegenType {
    private String type, format;

    @Override
    public boolean isDate() {
        return type.equals("string") && ("date".equals(format) || "date-time".equals(format));
    }
}

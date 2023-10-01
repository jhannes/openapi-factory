package org.openapifactory.api.codegen;

import lombok.Data;

@Data
public abstract class CodegenType {
    public boolean hasOnlyOptionalProperties() {
        return false;
    }
}

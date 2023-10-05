package org.openapifactory.api.codegen;

public interface CodegenType {
    default boolean hasOnlyOptionalProperties() {
        return false;
    }
}

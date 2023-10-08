package org.openapifactory.api.codegen;

import org.openapifactory.api.codegen.types.CodegenSchema;

public interface CodegenProp {
    String getName();

    String getDescription();

    CodegenSchema getSchema();

    boolean isRequired();

    default boolean isNullable() {
        return false;
    }
}

package org.openapifactory.api.codegen;

import org.openapifactory.api.codegen.types.CodegenType;

public interface CodegenProp {
    String getName();

    String getDescription();

    CodegenType getType();

    boolean isRequired();

    default boolean isNullable() {
        return false;
    }
}

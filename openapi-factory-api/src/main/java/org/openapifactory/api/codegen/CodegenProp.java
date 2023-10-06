package org.openapifactory.api.codegen;

import org.openapifactory.api.codegen.types.CodegenType;

public interface CodegenProp {
    String getName();

    boolean isRequired();

    CodegenType getType();
}

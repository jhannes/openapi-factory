package org.openapifactory.api.codegen;

public interface CodegenProp {
    String getName();

    boolean isRequired();

    CodegenType getType();
}

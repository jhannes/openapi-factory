package org.openapifactory.api.codegen;

public interface CodegenProp {
    String getName();

    boolean isRequired();

    CodegenType getType();

    default boolean isDate() {
        if (getType() instanceof CodegenPrimitiveType type) {
            return "string".equals(type.getType()) && "date".equals(type.getFormat());
        } else {
            return false;
        }
    }
}

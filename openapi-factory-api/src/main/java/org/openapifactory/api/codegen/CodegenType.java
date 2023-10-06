package org.openapifactory.api.codegen;

public interface CodegenType {
    default boolean hasNoRequiredProperties() {
        return true;
    }

    default boolean hasReadOnlyProperties() {
        return false;
    }

    default boolean hasWriteOnlyProperties() {
        return false;
    }

    default boolean isDate() {
        return false;
    }

    default CodegenType getReferencedType() {
        return this;
    }
}

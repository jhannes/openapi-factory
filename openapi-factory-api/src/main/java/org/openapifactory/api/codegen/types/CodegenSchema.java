package org.openapifactory.api.codegen.types;

public interface CodegenSchema {
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

    default CodegenSchema getReferencedType() {
        return this;
    }
}

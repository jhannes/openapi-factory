package org.openapifactory.api.codegen;

import java.util.Collection;

public interface CodegenEnum extends CodegenType {
    String getType();

    Collection<String> getValues();

    String getDescription();

    default boolean isString() {
        return getType().equals("string");
    }
}

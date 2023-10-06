package org.openapifactory.api.codegen;

import java.util.Collection;

public interface CodegenEnum {
    String getName();

    String getType();

    Collection<String> getValues();

    String getDescription();

    default boolean isString() {
        return getType().equals("string");
    }
}

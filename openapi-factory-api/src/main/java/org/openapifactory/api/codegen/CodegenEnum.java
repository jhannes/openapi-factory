package org.openapifactory.api.codegen;

import java.util.Collection;

public interface CodegenEnum {
    String getName();

    Collection<String> getValues();

    String getDescription();
}

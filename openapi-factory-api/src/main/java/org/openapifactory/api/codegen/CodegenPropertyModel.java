package org.openapifactory.api.codegen;

import org.openapifactory.api.Maybe;

import java.util.Collection;

public interface CodegenPropertyModel extends CodegenType {
    Collection<CodegenProperty> getAllProperties();
    CodegenProperty addProperty(String name);
    Maybe<CodegenProperty> getProperty(String name);
}

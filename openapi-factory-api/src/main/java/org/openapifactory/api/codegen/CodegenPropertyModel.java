package org.openapifactory.api.codegen;

import org.openapifactory.api.Maybe;

public interface CodegenPropertyModel extends CodegenModel {
    Maybe<CodegenProperty> getProperty(String name);
}

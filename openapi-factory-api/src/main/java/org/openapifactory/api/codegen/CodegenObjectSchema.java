package org.openapifactory.api.codegen;

import org.openapifactory.api.parser.Maybe;
import org.openapifactory.api.codegen.types.CodegenSchema;

import java.util.Collection;

public interface CodegenObjectSchema extends CodegenSchema {
    Collection<CodegenProperty> getAllProperties();
    CodegenProperty addProperty(String name);
    Maybe<CodegenProperty> getProperty(String name);

    void setAdditionalPropertiesFlag(Boolean additionalPropertiesFlag);
}

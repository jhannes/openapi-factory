package org.openapifactory.api.codegen;


import lombok.Data;
import org.openapifactory.api.Maybe;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class CodegenInlineObjectType implements CodegenType, CodegenPropertyMap, CodegenPropertyModel {
    private String name;
    private final Map<String, CodegenProperty> properties = new LinkedHashMap<>();

    @Override
    public boolean hasOnlyOptionalProperties() {
        return properties.values().stream().noneMatch(CodegenProperty::isRequired);
    }

    @Override
    public String getName() {
        if (name == null) {
            throw new UnsupportedOperationException("Can't find name of inline object");
        }
        return name;
    }

    @Override
    public CodegenProperty addProperty(String name) {
        var property = new CodegenProperty(name);
        properties.put(name, property);
        return property;
    }

    @Override
    public Maybe<CodegenProperty> getProperty(String name) {
        return Maybe.ofNullable(properties.get(name), "Missing " + name);
    }
}

package org.openapifactory.api.codegen;


import lombok.Data;
import lombok.ToString;
import org.openapifactory.api.Maybe;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class CodegenInlineObjectType implements CodegenPropertyModel {
    @ToString.Exclude
    private final OpenapiSpec spec;

    private String name;
    @ToString.Exclude
    private final Map<String, CodegenProperty> properties = new LinkedHashMap<>();

    @Override
    public boolean hasNoRequiredProperties() {
        return properties.values().stream().anyMatch(CodegenProperty::isRequired);
    }

    @Override
    public String getName() {
        if (name == null) {
            throw new UnsupportedOperationException("Can't find name of inline object");
        }
        return name;
    }

    public CodegenProperty addProperty(String name) {
        var property = new CodegenProperty(spec, name);
        properties.put(name, property);
        return property;
    }

    @Override
    public Maybe<CodegenProperty> getProperty(String name) {
        return Maybe.ofNullable(properties.get(name), "Missing " + name);
    }

    @Override
    public Collection<CodegenProperty> getAllProperties() {
        return properties.values();
    }
}

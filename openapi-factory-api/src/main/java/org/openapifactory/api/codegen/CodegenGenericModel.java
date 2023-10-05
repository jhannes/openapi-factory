package org.openapifactory.api.codegen;

import lombok.Data;
import org.openapifactory.api.Maybe;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class CodegenGenericModel implements CodegenModel, CodegenPropertyMap, CodegenPropertyModel {
    private final String name;
    private String description;

    private final Map<String, CodegenProperty> properties = new LinkedHashMap<>();

    @Override
    public CodegenProperty addProperty(String name) {
        var property = new CodegenProperty(name);
        properties.put(name, property);
        return property;
    }

    @Override
    public Maybe<CodegenProperty> getProperty(String name) {
        return Maybe.ofNullable(properties.get(name), "Missing property [" + name + "] in " + getName());
    }
}

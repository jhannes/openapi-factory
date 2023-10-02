package org.openapifactory.api.codegen;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class CodegenGenericModel extends CodegenModel implements CodegenPropertyMap {
    private final String name;
    private String description;

    private final Map<String, CodegenProperty> properties = new LinkedHashMap<>();

    @Override
    public CodegenProperty addProperty(String name) {
        var property = new CodegenProperty(name);
        properties.put(name, property);
        return property;
    }
}

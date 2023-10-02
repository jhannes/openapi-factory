package org.openapifactory.api.codegen;


import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = false)
public class CodegenInlineObjectType extends CodegenType implements CodegenPropertyMap {
    private String declaredType, declaredProperty;
    private final Map<String, CodegenProperty> properties = new LinkedHashMap<>();

    @Override
    public boolean hasOnlyOptionalProperties() {
        return properties.values().stream().noneMatch(CodegenProperty::isRequired);
    }

    @Override
    public String getName() {
        throw new UnsupportedOperationException("Can't find name of inline object");
    }

    @Override
    public CodegenProperty addProperty(String name) {
        var property = new CodegenProperty(name);
        properties.put(name, property);
        return property;
    }
}

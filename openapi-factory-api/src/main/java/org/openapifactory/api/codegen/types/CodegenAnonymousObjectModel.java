package org.openapifactory.api.codegen.types;


import lombok.Data;
import lombok.ToString;
import org.openapifactory.api.parser.Maybe;
import org.openapifactory.api.codegen.CodegenProperty;
import org.openapifactory.api.codegen.CodegenObjectSchema;
import org.openapifactory.api.codegen.OpenapiSpec;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class CodegenAnonymousObjectModel implements CodegenObjectSchema {
    @ToString.Exclude
    private final OpenapiSpec spec;

    @ToString.Exclude
    private final Map<String, CodegenProperty> properties = new LinkedHashMap<>();

    private Boolean additionalPropertiesFlag;

    @Override
    public boolean hasNoRequiredProperties() {
        return properties.values().stream().anyMatch(CodegenProperty::isRequired);
    }

    public CodegenProperty addProperty(String name) {
        var property = new CodegenProperty(spec, this, name);
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

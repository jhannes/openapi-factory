package org.openapifactory.api.codegen;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.openapifactory.api.Maybe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@ToString
public class CodegenGenericModel implements CodegenPropertyModel {
    @Getter
    private final OpenapiSpec spec;
    @Getter
    private final String name;
    @Getter @Setter
    private String description;

    private final Map<String, CodegenProperty> properties = new LinkedHashMap<>();

    @Override
    public CodegenProperty addProperty(String name) {
        var property = new CodegenProperty(spec, name);
        properties.put(name, property);
        return property;
    }

    @Override
    public Maybe<CodegenProperty> getProperty(String name) {
        return Maybe.ofNullable(properties.get(name), "Missing property [" + name + "] in " + getName());
    }

    @Override
    public Collection<CodegenProperty> getAllProperties() {
        return properties.values();
    }

    @Override
    public boolean hasReadOnlyProperties() {
        return !getOmittedPropertiesForReadOnly().isEmpty();
    }

    public List<CodegenProperty> getReadOnlyProperties() {
        return properties.values().stream()
                .filter(p -> p.isReadOnly() && p.isRequired())
                .toList();
    }

    public List<CodegenProperty> getReferencesWithReadOnlyProperties() {
        return properties.values().stream()
                .filter(p -> p.getType().hasReadOnlyProperties())
                .toList();
    }

    public List<CodegenProperty> getOmittedPropertiesForReadOnly() {
        var result = new ArrayList<>(getReadOnlyProperties());
        result.addAll(getReferencesWithReadOnlyProperties());
        return result;
    }
}

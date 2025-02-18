package org.openapifactory.api.codegen.types;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.openapifactory.api.codegen.CodegenXml;
import org.openapifactory.api.parser.Maybe;
import org.openapifactory.api.codegen.CodegenProperty;
import org.openapifactory.api.codegen.CodegenObjectSchema;
import org.openapifactory.api.codegen.OpenapiSpec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@ToString
public class CodegenGenericModel implements CodegenObjectSchema, CodegenModel {
    @ToString.Exclude
    private final OpenapiSpec spec;
    @Getter
    private final String name;
    @Getter @Setter
    private String description;

    @ToString.Exclude
    @Getter @Setter
    private CodegenXml xml;

    @Getter @Setter
    private Boolean additionalPropertiesFlag;

    @Getter
    private final Map<String, CodegenProperty> properties = new LinkedHashMap<>();

    @Override
    public CodegenProperty addProperty(String name) {
        var property = new CodegenProperty(spec, this, name);
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
                .filter(p -> p.getSchema().hasReadOnlyProperties())
                .toList();
    }

    public List<CodegenProperty> getOmittedPropertiesForReadOnly() {
        var result = new ArrayList<>(getReadOnlyProperties());
        result.addAll(getReferencesWithReadOnlyProperties());
        return result;
    }

    @Override
    public boolean hasWriteOnlyProperties() {
        return !getOmittedPropertiesForWriteOnly().isEmpty();
    }

    public List<CodegenProperty> getWriteOnlyProperties() {
        return properties.values().stream()
                .filter(p -> p.isWriteOnly() && p.isRequired())
                .toList();
    }

    public List<CodegenProperty> getReferencesWithWriteOnlyProperties() {
        return properties.values().stream()
                .filter(p -> p.getSchema().hasWriteOnlyProperties())
                .toList();
    }

    public List<CodegenProperty> getOmittedPropertiesForWriteOnly() {
        var result = new ArrayList<>(getWriteOnlyProperties());
        result.addAll(getReferencesWithWriteOnlyProperties());
        return result;
    }

}

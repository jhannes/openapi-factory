package org.openapifactory.api.codegen;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class CodegenOneOfModel implements CodegenModel {

    @ToString
    public static class Discriminator {
        @Getter @Setter
        private String propertyName;
        private final Map<String, CodegenTypeRef> mapping = new LinkedHashMap<>();
    }

    @Data
    public static class Mapping {
        private final String name;
        private final CodegenModel type;
    }

    @ToString.Exclude
    private final OpenapiSpec spec;
    private final String name;
    private final Discriminator discriminator = new Discriminator();
    private final List<CodegenTypeRef> oneOf = new ArrayList<>();

    public void addOneOf(String $ref) {
        oneOf.add(new CodegenTypeRef(spec, $ref));
    }

    public void addMapping(String discriminatorValue, String $ref) {
        discriminator.mapping.put(discriminatorValue, new CodegenTypeRef(spec, $ref));
    }

    public Collection<Mapping> getMappedModels() {
        if (!discriminator.mapping.isEmpty()) {
            return discriminator.mapping.entrySet().stream()
                    .map(entry -> new Mapping(entry.getKey(), (CodegenModel) entry.getValue().getReferencedType()))
                    .toList();
        }
        if (discriminator.propertyName == null) {
            throw new RuntimeException();
        }
        return oneOf.stream().map(ref -> {
                    var model = spec.getModel(ref);
                    if (model instanceof CodegenOneOfModel oneOfModel) {
                        return new Mapping(ref.getClassName(), oneOfModel);
                    }
                    var mappedName = ((CodegenPropertyModel)model).getProperty(discriminator.propertyName)
                            .map(CodegenProperty::getType)
                            .filterType(CodegenConstantType.class)
                            .map(CodegenConstantType::getValue)
                            .orElse(ref.getClassName());
                    return new Mapping(mappedName, model);
                }).toList();
    }

    public Collection<? extends CodegenType> getModels() {
        if (discriminator.mapping.isEmpty()) {
            return oneOf;
        } else {
            return discriminator.mapping.values();
        }
    }

    @Override
    public boolean hasReadOnlyProperties() {
        return false && getModels().stream().anyMatch(CodegenType::hasReadOnlyProperties);
    }


}

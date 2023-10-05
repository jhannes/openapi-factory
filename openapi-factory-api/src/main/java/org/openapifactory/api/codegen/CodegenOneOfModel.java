package org.openapifactory.api.codegen;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class CodegenOneOfModel implements CodegenModel {

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

    private final OpenapiSpec spec;
    private final String name;
    private final Discriminator discriminator = new Discriminator();
    private final List<CodegenTypeRef> oneOf = new ArrayList<>();

    public void addOneOf(String $ref) {
        oneOf.add(new CodegenTypeRef($ref));
    }

    public void addMapping(String discriminatorValue, String $ref) {
        discriminator.mapping.put(discriminatorValue, new CodegenTypeRef($ref));
    }

    public Collection<Mapping> getMappedModels() {
        if (!discriminator.mapping.isEmpty()) {
            return discriminator.mapping.entrySet().stream()
                    .map(entry -> new Mapping(entry.getKey(), (CodegenPropertyModel) spec.getModel(entry.getValue())))
                    .toList();
        }
        if (discriminator.propertyName == null) {
            throw new RuntimeException();
        }
        return oneOf.stream().map(ref -> {
                    var model = spec.getModel(ref);
                    if (model instanceof CodegenOneOfModel oneOf) {
                        return new Mapping(ref.getClassName(), oneOf);
                    }
                    var mappedName = ((CodegenPropertyModel)model).getProperty(discriminator.propertyName)
                            .map(CodegenProperty::getType)
                            .filterType(CodegenInlineEnumType.class)
                            .filter(m -> m.getValues().size() == 1, "enum should have only one value")
                            .map(m -> m.getValues().get(0))
                            .orElse(ref.getClassName());
                    return new Mapping(mappedName, model);
                }).toList();
    }



}

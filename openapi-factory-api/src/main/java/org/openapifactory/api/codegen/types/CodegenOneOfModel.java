package org.openapifactory.api.codegen.types;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.openapifactory.api.codegen.CodegenProperty;
import org.openapifactory.api.codegen.CodegenObjectSchema;
import org.openapifactory.api.codegen.OpenapiSpec;

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
        private final Map<String, CodegenSchemaRef> mapping = new LinkedHashMap<>();
    }

    @Data
    public static class Mapping {
        private final String name;
        private final CodegenSchema schema;
    }

    @ToString.Exclude
    private final OpenapiSpec spec;
    private final String name;
    private final Discriminator discriminator = new Discriminator();
    private final List<CodegenSchemaRef> oneOf = new ArrayList<>();

    public void addOneOf(String $ref, String relativeFilename) {
        oneOf.add(new CodegenSchemaRef(spec, $ref, relativeFilename));
    }

    public void addMapping(String discriminatorValue, CodegenSchemaRef schemaRef) {
        discriminator.mapping.put(discriminatorValue, schemaRef);
    }

    public Collection<Mapping> getMappedModels() {
        if (!discriminator.mapping.isEmpty()) {
            return discriminator.mapping.entrySet().stream()
                    .map(entry -> new Mapping(entry.getKey(), entry.getValue()))
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
                    var mappedName = ((CodegenObjectSchema)model).getProperty(discriminator.propertyName)
                            .map(CodegenProperty::getSchema)
                            .filterType(CodegenConstantSchema.class)
                            .map(CodegenConstantSchema::getValue)
                            .orElse(ref.getClassName());
                    return new Mapping(mappedName, model);
                }).toList();
    }

    public Collection<? extends CodegenSchema> getModels() {
        if (discriminator.mapping.isEmpty()) {
            return oneOf;
        } else {
            return discriminator.mapping.values();
        }
    }

    @Override
    public boolean hasReadOnlyProperties() {
        return getModels().stream().anyMatch(CodegenSchema::hasReadOnlyProperties);
    }


}

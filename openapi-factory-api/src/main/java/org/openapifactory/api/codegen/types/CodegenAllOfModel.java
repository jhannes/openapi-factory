package org.openapifactory.api.codegen.types;

import lombok.Data;
import lombok.ToString;
import org.openapifactory.api.parser.Maybe;
import org.openapifactory.api.codegen.CodegenProperty;
import org.openapifactory.api.codegen.CodegenObjectSchema;
import org.openapifactory.api.codegen.OpenapiSpec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Data
public class CodegenAllOfModel implements CodegenObjectSchema, CodegenModel {
    @ToString.Exclude
    private final OpenapiSpec spec;
    private final String name;
    private final List<CodegenSchemaRef> refSuperModels = new ArrayList<>();
    private final List<CodegenGenericModel> inlineSuperModels = new ArrayList<>();
    private final Set<String> required = new LinkedHashSet<>();
    private Boolean additionalPropertiesFlag;

    public void addRefSuperModel(String ref, String relativeFilename) {
        refSuperModels.add(new CodegenSchemaRef(spec, ref, relativeFilename));
    }

    public CodegenGenericModel addSuperModel() {
        var result = new CodegenGenericModel(spec, getName());
        inlineSuperModels.add(result);
        return result;
    }

    public List<CodegenProperty> getOwnProperties() {
        var result = new ArrayList<CodegenProperty>();
        for (var name : required) {
            if (inlineSuperModels.stream().noneMatch(m -> m.getProperty(name).isPresent())) {
                var property = getProperty(name).required().clone();
                property.setRequired(true);
                result.add(property);
            }
        }
        inlineSuperModels.forEach(m -> result.addAll(m.getAllProperties()));
        return result;
    }

    @Override
    public Maybe<CodegenProperty> getProperty(String name) {
        for (var inlineSuperModel : inlineSuperModels) {
            var match = inlineSuperModel.getProperty(name);
            if (match.isPresent()) {
                 return match;
            }
        }
        for (var refSuperModel : refSuperModels) {
            var superModel = (CodegenObjectSchema)refSuperModel.getReferencedType();
            var match = superModel.getProperty(name);
            if (match.isPresent()) {
                return match;
            }
        }
        return Maybe.missing("No property [" + name + "] in " + getName());
    }

    @Override
    public CodegenProperty addProperty(String name) {
        throw new RuntimeException("Should be implemented");
    }

    @Override
    public boolean hasReadOnlyProperties() {
        return !getOmittedPropertiesForReadOnly().isEmpty();
    }

    @Override
    public Collection<CodegenProperty> getAllProperties() {
        var result = new ArrayList<CodegenProperty>();
        getRefSuperModels().stream()
                .map(superModel -> (CodegenObjectSchema)superModel.getReferencedType())
                .forEach(superModel -> result.addAll(superModel.getAllProperties()));
        result.addAll(getOwnProperties());
        return result;
    }

    public List<CodegenProperty> getReferencesWithReadOnlyProperties() {
        return getAllProperties().stream()
                .filter(p -> p.getSchema().hasReadOnlyProperties())
                .toList();
    }

    public List<CodegenProperty> getOmittedPropertiesForReadOnly() {
        return getAllProperties().stream()
                .filter(p -> p.getSchema().hasReadOnlyProperties() || (p.isReadOnly() && p.isRequired()))
                .toList();
    }
}

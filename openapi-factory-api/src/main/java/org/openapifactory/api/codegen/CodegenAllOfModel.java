package org.openapifactory.api.codegen;

import lombok.Data;
import lombok.ToString;
import org.openapifactory.api.Maybe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Data
public class CodegenAllOfModel implements CodegenPropertyModel, CodegenModel {
    @ToString.Exclude
    private final OpenapiSpec spec;
    private final String name;
    private final List<CodegenTypeRef> refSuperModels = new ArrayList<>();
    private final List<CodegenGenericModel> inlineSuperModels = new ArrayList<>();
    private final Set<String> required = new LinkedHashSet<>();

    public void addRefSuperModel(String ref) {
        refSuperModels.add(new CodegenTypeRef(spec, ref));
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
            var superModel = (CodegenPropertyModel)refSuperModel.getReferencedType();
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
                .map(superModel -> (CodegenPropertyModel)superModel.getReferencedType())
                .forEach(superModel -> result.addAll(superModel.getAllProperties()));
        result.addAll(getOwnProperties());
        return result;
    }

    public List<CodegenProperty> getReferencesWithReadOnlyProperties() {
        return getAllProperties().stream()
                .filter(p -> p.getType().hasReadOnlyProperties())
                .toList();
    }

    public List<CodegenProperty> getOmittedPropertiesForReadOnly() {
        return getAllProperties().stream()
                .filter(p -> p.getType().hasReadOnlyProperties() || (p.isReadOnly() && p.isRequired()))
                .toList();
    }
}

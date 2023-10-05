package org.openapifactory.api.codegen;

import lombok.Data;
import lombok.ToString;
import org.openapifactory.api.Maybe;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Data
public class CodegenAllOfModel implements CodegenModel, CodegenPropertyModel {
    @ToString.Exclude
    private final OpenapiSpec spec;
    private final String name;
    private final List<CodegenTypeRef> refSuperModels = new ArrayList<>();
    private final List<CodegenInlineObjectType> inlineSuperModels = new ArrayList<>();
    private final Set<String> required = new LinkedHashSet<>();

    public void addRefSuperModel(String ref) {
        refSuperModels.add(new CodegenTypeRef(ref));
    }

    public CodegenInlineObjectType addSuperModel() {
        var result = new CodegenInlineObjectType();
        result.setName(getName());
        inlineSuperModels.add(result);
        return result;
    }

    public List<CodegenProperty> getOwnProperties() {
        var result = new ArrayList<CodegenProperty>();
        for (var name : required) {
            if (inlineSuperModels.stream().noneMatch(m -> m.getProperties().containsKey(name))) {
                var property = getProperty(name).required().clone();
                property.setRequired(true);
                result.add(property);
            }
        }
        inlineSuperModels.forEach(m -> result.addAll(m.getProperties().values()));
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
            var superModel = (CodegenPropertyModel)spec.getModel(refSuperModel);
            var match = superModel.getProperty(name);
            if (match.isPresent()) {
                return match;
            }
        }
        return Maybe.missing("No property [" + name + "] in " + getName());
    }
}

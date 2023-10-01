package org.openapifactory.api.codegen;


import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = false)
public class CodegenInlineObjectType extends CodegenType {
    private String declaredType, declaredProperty;
    private final Map<String, CodegenProperty> properties = new LinkedHashMap<>();

    @Override
    public boolean hasOnlyOptionalProperties() {
        return properties.values().stream().noneMatch(CodegenProperty::isRequired);
    }
}

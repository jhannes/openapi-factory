package org.openapifactory.api.codegen;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

import static org.openapifactory.api.StringUtil.toUpperCamelCase;

@Data
public class CodegenInlineEnumType implements CodegenType {
    private String type;
    private CodegenPropertyMap declaredModel;
    private CodegenProp declaredProperty;
    private final List<String> values = new ArrayList<>();

    public String getTypeName() {
        if (getValues().size() > 1) {
            return toUpperCamelCase(getDeclaredModel().getName()) + toUpperCamelCase(getDeclaredProperty().getName()) + "Enum";
        } else {
            return "\"" + getValues().get(0) + "\"";
        }
    }
}

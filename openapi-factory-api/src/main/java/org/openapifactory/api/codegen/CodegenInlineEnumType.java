package org.openapifactory.api.codegen;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

import static org.openapifactory.api.StringUtil.toUpperCamelCase;

@Data
public class CodegenInlineEnumType implements CodegenModel {
    private String type;
    private CodegenPropertyMap declaredModel;
    private CodegenProp declaredProperty;
    private final List<String> values = new ArrayList<>();

    public String getName() {
        return toUpperCamelCase(getDeclaredModel().getName()) + toUpperCamelCase(getDeclaredProperty().getName()) + "Enum";
    }
}

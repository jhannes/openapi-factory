package org.openapifactory.api.codegen;

import lombok.Data;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

import static org.openapifactory.api.StringUtil.join;
import static org.openapifactory.api.StringUtil.toUpperCamelCase;

@Data
public class CodegenInlineEnumType implements CodegenModel, CodegenEnum {
    private String type;
    @ToString.Exclude
    private CodegenPropertyModel declaredModel;
    @ToString.Exclude
    private CodegenProp declaredProperty;
    private final List<String> values = new ArrayList<>();
    private String description;

    public String getName() {
        if (declaredModel == null) {
            return join(" | ", values, s -> "\"" + s + "\"");
        }
        return toUpperCamelCase(getDeclaredModel().getName()) + toUpperCamelCase(getDeclaredProperty().getName()) + "Enum";
    }
}

package org.openapifactory.api.codegen;

import lombok.Data;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Data
public class CodegenInlineEnumType implements CodegenEnum {
    private String type;
    @ToString.Exclude
    private CodegenPropertyModel declaredModel;
    @ToString.Exclude
    private CodegenProp declaredProperty;
    private final List<String> values = new ArrayList<>();
    private String description;
}

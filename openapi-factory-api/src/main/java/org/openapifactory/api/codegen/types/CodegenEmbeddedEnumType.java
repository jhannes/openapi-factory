package org.openapifactory.api.codegen.types;

import lombok.Data;
import lombok.ToString;
import org.openapifactory.api.codegen.CodegenProp;
import org.openapifactory.api.codegen.CodegenPropertyModel;

import java.util.ArrayList;
import java.util.List;

@Data
public class CodegenEmbeddedEnumType implements CodegenEnum {
    private String type;
    @ToString.Exclude
    private CodegenPropertyModel declaredModel;
    @ToString.Exclude
    private CodegenProp declaredProperty;
    private final List<String> values = new ArrayList<>();
    private String description;
}

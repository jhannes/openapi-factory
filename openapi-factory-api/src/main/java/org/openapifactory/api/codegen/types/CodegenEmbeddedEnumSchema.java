package org.openapifactory.api.codegen.types;

import lombok.Data;
import lombok.ToString;
import org.openapifactory.api.codegen.CodegenProp;

import java.util.ArrayList;
import java.util.List;

@Data
public class CodegenEmbeddedEnumSchema implements CodegenEnum {
    private String type;
    @ToString.Exclude
    private CodegenProp declaredProperty;
    private final List<String> values = new ArrayList<>();
    private String description;
}

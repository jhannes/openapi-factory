package org.openapifactory.api.codegen;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
public class CodegenInlineEnumType extends CodegenType {
    private String type;
    private String declaredType, declaredProperty;
    private final List<String> values = new ArrayList<>();
}

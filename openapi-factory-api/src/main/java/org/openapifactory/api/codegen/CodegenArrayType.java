package org.openapifactory.api.codegen;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Data
@EqualsAndHashCode(callSuper = false)
public class CodegenArrayType extends CodegenType {
    private boolean uniqueItems;
    private CodegenType items;

}

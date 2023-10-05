package org.openapifactory.api.codegen;

import lombok.Data;

@Data
public class CodegenArrayType implements CodegenType {
    private boolean uniqueItems;
    private CodegenType items;

}

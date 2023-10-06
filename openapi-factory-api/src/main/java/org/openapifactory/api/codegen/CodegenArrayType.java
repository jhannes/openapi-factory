package org.openapifactory.api.codegen;

import lombok.Data;

@Data
public class CodegenArrayType implements CodegenType {
    private boolean uniqueItems;
    private CodegenType items;

    @Override
    public boolean hasReadOnlyProperties() {
        return items.hasReadOnlyProperties();
    }

    @Override
    public boolean isDate() {
        return items.isDate();
    }
}

package org.openapifactory.api.codegen.types;

import lombok.Data;

@Data
public class CodegenArrayType implements CodegenType {
    private boolean uniqueItems;
    private CodegenType items;

    @Override
    public boolean hasNoRequiredProperties() {
        return items.hasNoRequiredProperties();
    }

    @Override
    public boolean hasReadOnlyProperties() {
        return items.hasReadOnlyProperties();
    }

    @Override
    public boolean hasWriteOnlyProperties() {
        return items.hasWriteOnlyProperties();
    }

    @Override
    public boolean isDate() {
        return items.isDate();
    }
}

package org.openapifactory.api.codegen.types;

import lombok.Data;

@Data
public class CodegenArraySchema implements CodegenSchema {
    private boolean uniqueItems;
    private CodegenSchema items;
    private int minItems = 0;
    private Integer maxItems;

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

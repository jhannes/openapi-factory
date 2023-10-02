package org.openapifactory.api.codegen;

import lombok.Data;

@Data
public class CodegenContent implements CodegenProp {
    private final String contentType;
    private CodegenType type;
    private boolean required;

    @Override
    public String getName() {
        return isFormContent() ? "formParams" : null;
    }

    public boolean isFormContent() {
        return contentType.equals("application/x-www-form-urlencoded");
    }
}

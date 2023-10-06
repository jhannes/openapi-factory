package org.openapifactory.api.codegen;

import lombok.Data;
import lombok.ToString;

@Data
public class CodegenContent {
    @ToString.Exclude
    private final OpenapiSpec spec;
    private final String contentType;
    @ToString.Exclude
    private CodegenType type;
    private boolean required;

    public boolean isFormContent() {
        return contentType.equals("application/x-www-form-urlencoded")
                || contentType.equals("multipart/form-data");
    }
}

package org.openapifactory.api.codegen;

import lombok.Data;
import lombok.ToString;
import org.openapifactory.api.codegen.types.CodegenSchema;

@Data
public class CodegenContent {
    @ToString.Exclude
    private final OpenapiSpec spec;
    private final String contentType;
    @ToString.Exclude
    private CodegenSchema schema;
    private boolean required;

    public boolean isFormContent() {
        return contentType.startsWith("application/x-www-form-urlencoded")
                || contentType.startsWith("multipart/form-data");
    }
}

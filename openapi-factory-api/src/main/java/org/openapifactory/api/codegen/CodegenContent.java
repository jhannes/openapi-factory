package org.openapifactory.api.codegen;

import lombok.Data;
import lombok.ToString;
import org.openapifactory.api.StringUtil;

@Data
public class CodegenContent implements CodegenProp {
    private final String contentType;
    @ToString.Exclude
    private CodegenType type;
    private boolean required;

    @Override
    public String getName() {
        if (isFormContent()) {
            return "formParams";
        } else if (type instanceof CodegenTypeRef typeRef) {
            return StringUtil.toLowerCamelCase(typeRef.getClassName()) + "Dto";
        } else if (type instanceof CodegenArrayType arrayType) {
            if (arrayType.getItems() instanceof CodegenTypeRef typeRef) {
                return StringUtil.toLowerCamelCase(typeRef.getClassName()) + "Dto";
            }
        }
        return null;
    }

    public boolean isFormContent() {
        return contentType.equals("application/x-www-form-urlencoded")
                || contentType.equals("multipart/form-data");
    }
}

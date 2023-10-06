package org.openapifactory.api.codegen;

import lombok.Data;
import lombok.ToString;
import org.openapifactory.api.codegen.types.CodegenType;

@Data
public class CodegenParameter implements CodegenProp {
    @ToString.Exclude
    private final OpenapiSpec spec;

    public enum ParameterLocation {
        cookie, header, path, query
    }
    public enum Style {
        deepObject, form, label, matrix, pipeDelimited, simple, spaceDelimited
    }

    private final String name;
    private boolean required;
    private CodegenType type;
    private ParameterLocation in;
    private Style style;
    private boolean explode = true;
}

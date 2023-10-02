package org.openapifactory.api.codegen;

import lombok.Data;

@Data
public class CodegenParameter implements CodegenProp {
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

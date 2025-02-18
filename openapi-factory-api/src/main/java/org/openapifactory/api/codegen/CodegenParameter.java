package org.openapifactory.api.codegen;

import lombok.Data;
import lombok.ToString;
import org.openapifactory.api.codegen.types.CodegenSchema;

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
    private String description;
    private boolean required;
    private CodegenSchema schema;
    private ParameterLocation in;
    private Style style;
    private boolean explode = true;
}

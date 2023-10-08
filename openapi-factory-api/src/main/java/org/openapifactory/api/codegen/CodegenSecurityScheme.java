package org.openapifactory.api.codegen;

import lombok.Data;

@Data
public class CodegenSecurityScheme {
    public enum Location {
        cookie, header, query
    }
    private final String key;
    private String type, name;
    private Location in;
}

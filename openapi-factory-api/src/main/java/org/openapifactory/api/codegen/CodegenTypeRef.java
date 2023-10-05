package org.openapifactory.api.codegen;

import lombok.Data;

import java.util.regex.Pattern;

@Data
public class CodegenTypeRef implements CodegenType {
    private final String ref;

    public CodegenTypeRef(String ref) {
        var refPattern = Pattern.compile("#/components/schemas/(?<schema>.*)");
        this.ref = ref;

        var match = refPattern.matcher(ref);
        if (!match.matches()) {
            throw new IllegalArgumentException("Invalid format for $ref: " + ref);
        }
    }

    public String getClassName() {
        var lastSlash = ref.lastIndexOf("/");
        return ref.substring(lastSlash + 1);
    }
}

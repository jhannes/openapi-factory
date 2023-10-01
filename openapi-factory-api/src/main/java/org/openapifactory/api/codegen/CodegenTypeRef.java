package org.openapifactory.api.codegen;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.regex.Pattern;

@Data
@EqualsAndHashCode(callSuper = false)
public class CodegenTypeRef extends CodegenType {
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

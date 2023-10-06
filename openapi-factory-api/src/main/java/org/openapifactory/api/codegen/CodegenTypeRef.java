package org.openapifactory.api.codegen;

import lombok.Data;
import lombok.ToString;

import java.util.regex.Pattern;

@Data
public class CodegenTypeRef implements CodegenType {
    public static final Pattern REF_PATTERN = Pattern.compile("#/components/schemas/(?<schema>.*)");
    @ToString.Exclude
    private final OpenapiSpec spec;
    private final String ref;

    public CodegenTypeRef(OpenapiSpec spec, String ref) {
        this.spec = spec;
        this.ref = ref;

        var match = REF_PATTERN.matcher(ref);
        if (!match.matches()) {
            throw new IllegalArgumentException("Invalid format for $ref: " + ref);
        }
    }

    public String getClassName() {
        var lastSlash = ref.lastIndexOf("/");
        return ref.substring(lastSlash + 1);
    }

    @Override
    public boolean hasReadOnlyProperties() {
        return getReferencedType().hasReadOnlyProperties();
    }

    @Override
    public CodegenType getReferencedType() {
        return spec.getModel(this);
    }
}

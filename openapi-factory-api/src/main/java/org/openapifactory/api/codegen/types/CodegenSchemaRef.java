package org.openapifactory.api.codegen.types;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.openapifactory.api.codegen.OpenapiSpec;

import java.util.regex.Pattern;

@ToString
@Getter
@EqualsAndHashCode(of = {"ref"})
public class CodegenSchemaRef implements CodegenSchema {
    public static final Pattern REF_PATTERN = Pattern.compile("#/components/schemas/(?<schema>.*)");
    @ToString.Exclude
    private final OpenapiSpec spec;
    private final String ref;

    public CodegenSchemaRef(OpenapiSpec spec, String ref, String relativeFilename) {
        this.spec = spec;
        this.ref = ref.startsWith("#") ? relativeFilename + ref : ref;

        var match = Pattern.compile("[-_.#/a-zA-Z0-9]+").matcher(ref);
        if (!match.matches()) {
            throw new IllegalArgumentException("Invalid format for $ref: " + ref);
        }
        spec.addReference(this);
    }

    public String getClassName() {
        var lastSlash = ref.lastIndexOf("/");
        return ref.substring(lastSlash + 1);
    }

    @Override
    public boolean hasNoRequiredProperties() {
        return getReferencedType().hasNoRequiredProperties();
    }

    @Override
    public boolean hasReadOnlyProperties() {
        return getReferencedType().hasReadOnlyProperties();
    }

    @Override
    public boolean hasWriteOnlyProperties() {
        return getReferencedType().hasWriteOnlyProperties();
    }

    @Override
    public CodegenSchema getReferencedType() {
        return spec.getModel(this);
    }
}

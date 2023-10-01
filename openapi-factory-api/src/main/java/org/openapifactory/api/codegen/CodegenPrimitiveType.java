package org.openapifactory.api.codegen;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class CodegenPrimitiveType extends CodegenType {
    private String type, format;
}

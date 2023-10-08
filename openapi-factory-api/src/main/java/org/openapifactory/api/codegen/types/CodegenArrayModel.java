package org.openapifactory.api.codegen.types;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CodegenArrayModel extends CodegenArraySchema implements CodegenModel {
    private final String name;
    private String description;
}

package org.openapifactory.api.codegen;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public abstract class CodegenModel {
    private String name;
    private String description;

    private final Map<String, CodegenProperty> properties = new LinkedHashMap<>();
}

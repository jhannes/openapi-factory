package org.openapifactory.api.codegen;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CodegenEnumModel implements CodegenEnum, CodegenModel {
    private final String name;
    private String type;
    private final List<String> values = new ArrayList<>();
    private String description;
}

package org.openapifactory.api.codegen;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CodegenSecurity {
    private final String name;
    private List<String> scopes = new ArrayList<>();
}

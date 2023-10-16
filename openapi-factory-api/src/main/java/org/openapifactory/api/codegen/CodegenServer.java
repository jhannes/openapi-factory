package org.openapifactory.api.codegen;

import lombok.Data;

import java.util.Optional;

@Data
public class CodegenServer {
    private Optional<String> description = Optional.empty();
    private String url = "";
}

package org.openapifactory.api.codegen;

import lombok.Data;

@Data
public class CodegenXml {
    private String name, namespace, prefix;
    private boolean attribute, wrapped;
}

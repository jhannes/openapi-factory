package org.openapifactory.api.codegen;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class CodegenOneOfModel extends CodegenModel {
    @Data
    public static class Discriminator {
        private String propertyName;
        private final Map<String, CodegenType> mapping = new LinkedHashMap<>();
    }
    private final Discriminator discriminator = new Discriminator();
}

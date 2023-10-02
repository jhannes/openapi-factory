package org.openapifactory.api.codegen;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
public class CodegenEnumModel extends CodegenModel {
    private final String name;
    private final List<String> values = new ArrayList<>();
}

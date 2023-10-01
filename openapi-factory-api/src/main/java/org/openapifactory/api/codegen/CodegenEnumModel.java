package org.openapifactory.api.codegen;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CodegenEnumModel extends CodegenModel {
    private final List<String> values = new ArrayList<>();

}

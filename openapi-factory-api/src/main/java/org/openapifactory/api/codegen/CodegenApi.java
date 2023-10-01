package org.openapifactory.api.codegen;

import lombok.Data;
import org.openapifactory.api.StringUtil;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

@Data
public class CodegenApi {
    private String tag;
    private Map<String, CodegenOperation> operations = new TreeMap<>();

    public CodegenApi(String tag) {
        this.tag = tag;
    }

    public Collection<CodegenOperation> getOperations() {
        return operations.values();
    }

    public void addOperation(CodegenOperation codegenOperation) {
        this.operations.put(codegenOperation.getOperationId(), codegenOperation);
    }

    public String getApiName() {
        return StringUtil.toUpperCamelCase(tag) + "Api";
    }
}

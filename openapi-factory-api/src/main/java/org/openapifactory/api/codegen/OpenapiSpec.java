package org.openapifactory.api.codegen;

import lombok.Data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;

@Data
public class OpenapiSpec {
    private String name, title, description, version;
    private Optional<CodegenContact> contact = Optional.empty();

    private List<CodegenServer> servers = new ArrayList<>();
    private Map<String, CodegenApi> apiMap = new TreeMap<>();
    private Map<String, CodegenModel> modelMap = new LinkedHashMap<>();

    public Collection<CodegenApi> getApis() {
        return apiMap.values();
    }
    public Collection<CodegenModel> getModels() {
        return new TreeSet<>(modelMap.keySet())
                .stream().map(m -> modelMap.get(m))
                .toList();
    }

    public void addOperation(String tag, CodegenOperation codegenOperation) {
        apiMap.computeIfAbsent(tag, CodegenApi::new).addOperation(codegenOperation);
    }
}

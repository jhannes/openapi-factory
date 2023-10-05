package org.openapifactory.api.codegen;

import lombok.Data;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;

@Data
@ToString(of={"name", "title", "description", "version"})
public class OpenapiSpec {
    private final String modelSuffix = "Dto";
    private String name, title, description, version;
    private Optional<CodegenContact> contact = Optional.empty();

    private List<CodegenServer> servers = new ArrayList<>();
    private Map<String, CodegenApi> apiMap = new TreeMap<>();
    private Map<String, CodegenModel> modelMap = new LinkedHashMap<>();
    private List<CodegenSecurityScheme> securitySchemes = new ArrayList<>();

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

    public CodegenServer addServer() {
        var server = new CodegenServer();
        servers.add(server);
        return server;
    }

    public CodegenOperation createOperation(String method, String path) {
        return new CodegenOperation(method, path);
    }

    public CodegenGenericModel addGenericModel(String modelName) {
        return addModel(new CodegenGenericModel(modelName + modelSuffix));
    }

    public CodegenEnumModel addEnumModel(String modelName) {
        return addModel(new CodegenEnumModel(modelName + modelSuffix));
    }

    public CodegenOneOfModel addOneOfModel(String modelName) {
        return addModel(new CodegenOneOfModel(this, modelName + modelSuffix));
    }

    public CodegenAllOfModel addAllOfModel(String modelName) {
        return addModel(new CodegenAllOfModel(this, modelName + modelSuffix));
    }

    public CodegenSecurityScheme addSecurityScheme(String scheme) {
        var securityScheme = new CodegenSecurityScheme(scheme);
        this.securitySchemes.add(securityScheme);
        return securityScheme;
    }

    public CodegenModel getModel(CodegenTypeRef ref) {
        return modelMap.get(ref.getClassName() + modelSuffix);
    }

    private <T extends CodegenModel> T addModel(T model) {
        modelMap.put(model.getName(), model);
        return model;
    }
}

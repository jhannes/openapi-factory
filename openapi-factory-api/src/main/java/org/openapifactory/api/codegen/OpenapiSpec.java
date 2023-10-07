package org.openapifactory.api.codegen;

import lombok.Data;
import lombok.ToString;
import org.openapifactory.api.codegen.types.CodegenAllOfModel;
import org.openapifactory.api.codegen.types.CodegenArrayModel;
import org.openapifactory.api.codegen.types.CodegenEnumModel;
import org.openapifactory.api.codegen.types.CodegenGenericModel;
import org.openapifactory.api.codegen.types.CodegenModel;
import org.openapifactory.api.codegen.types.CodegenOneOfModel;
import org.openapifactory.api.codegen.types.CodegenTypeRef;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

@Data
@ToString(of={"name", "title", "description", "version"})
public class OpenapiSpec {
    public static final Pattern EXTERNAL_REF = Pattern.compile("^(?<filename>\\.((/[-_.a-zA-Z0-9]+)+))#(?<anchor>(/[A-Za-z0-9_]+)+)$");
    private String name, title, description, version;
    private Optional<CodegenContact> contact = Optional.empty();

    private final List<CodegenServer> servers = new ArrayList<>();
    private final Map<String, CodegenApi> apiMap = new TreeMap<>();
    private final Map<String, CodegenModel> modelMap = new LinkedHashMap<>();
    private final List<CodegenSecurityScheme> securitySchemes = new ArrayList<>();
    private final List<CodegenTypeRef> typeReferences = new ArrayList<>();
    private final Map<CodegenTypeRef, CodegenModel> resolvedModels = new LinkedHashMap<>();

    public Collection<CodegenApi> getApis() {
        return apiMap.values();
    }
    public Collection<CodegenModel> getModels() {
        return new TreeSet<>(modelMap.keySet())
                .stream().map(modelMap::get)
                .filter(m -> !(m instanceof CodegenArrayModel))
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
        return new CodegenOperation(this, method, path);
    }

    public CodegenGenericModel addGenericModel(String modelName) {
        return addModel(new CodegenGenericModel(this, modelName));
    }

    public CodegenEnumModel addEnumModel(String modelName) {
        return addModel(new CodegenEnumModel(modelName));
    }

    public CodegenOneOfModel addOneOfModel(String modelName) {
        return addModel(new CodegenOneOfModel(this, modelName));
    }

    public CodegenAllOfModel addAllOfModel(String modelName) {
        return addModel(new CodegenAllOfModel(this, modelName));
    }

    public CodegenArrayModel addArrayModel(String modelName) {
        return addModel(new CodegenArrayModel(modelName));
    }

    public CodegenSecurityScheme addSecurityScheme(String scheme) {
        var securityScheme = new CodegenSecurityScheme(scheme);
        this.securitySchemes.add(securityScheme);
        return securityScheme;
    }

    public CodegenModel getModel(CodegenTypeRef ref) {
        if (resolvedModels.containsKey(ref)) {
            return resolvedModels.get(ref);
        }
        if (!modelMap.containsKey(ref.getClassName())) {
            throw new IllegalArgumentException("Missing $ref " + ref.getClassName() + " in " + modelMap.keySet());
        }
        return modelMap.get(ref.getClassName());
    }

    private <T extends CodegenModel> T addModel(T model) {
        modelMap.put(model.getName(), model);
        return model;
    }

    public void addReference(CodegenTypeRef reference) {
        this.typeReferences.add(reference);
    }

    public Set<CodegenTypeRef> getUnresolvedTypeReferences() {
        var result = new HashSet<>(getTypeReferences());
        result.removeIf(r -> r.getRef().startsWith("#"));
        result.removeAll(getResolvedModels().keySet());
        return result;
    }
}

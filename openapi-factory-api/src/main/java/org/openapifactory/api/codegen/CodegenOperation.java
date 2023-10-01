package org.openapifactory.api.codegen;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.openapifactory.api.codegen.CodegenParameter.ParameterLocation.path;

@Data
public class CodegenOperation {
    private String operationId;
    private String method, path;


    private List<CodegenParameter> parameters = new ArrayList<>();
    private Map<String, CodegenContent> requestBodies = new LinkedHashMap<>();
    private Map<String, CodegenContent> responseTypes = new LinkedHashMap<>();

    public List<CodegenParameter> getPathParams() {
        return filterOnLocation(CodegenParameter.ParameterLocation.path);
    }
    public List<CodegenParameter> getQueryParams() {
        return filterOnLocation(CodegenParameter.ParameterLocation.query);
    }
    public List<CodegenParameter> getHeaderParams() {
        return filterOnLocation(CodegenParameter.ParameterLocation.header);
    }

    public CodegenContent getRequestBody() {
        return requestBodies.getOrDefault("application/json", requestBodies.get("application/x-www-form-urlencoded"));
    }

    public CodegenContent getResponseType() {
        return responseTypes.get("application/json");
    }

    private List<CodegenParameter> filterOnLocation(CodegenParameter.ParameterLocation location) {
        return parameters.stream().filter(codegenParameter -> codegenParameter.getIn().equals(location)).toList();
    }

    public boolean hasOnlyOptionalParams() {
        return parameters.stream().noneMatch(CodegenParameter::isRequired)  &&
               requestBodies.values().stream().noneMatch(CodegenContent::isRequired);
    }
}

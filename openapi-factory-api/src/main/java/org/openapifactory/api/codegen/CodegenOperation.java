package org.openapifactory.api.codegen;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.openapifactory.api.StringUtil.toUpperCamelCase;

@Data
public class CodegenOperation {
    private final String method, path;
    private String operationId;

    public CodegenOperation(String method, String path) {
        this.method = method.toUpperCase();
        this.path = path;
        this.operationId = inlinePathParams(camelCaseSlashes(path)) + toUpperCamelCase(method);
    }

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

    public boolean hasOnlyOptionalParams() {
        return parameters.stream().noneMatch(CodegenParameter::isRequired)  &&
               requestBodies.values().stream().noneMatch(CodegenContent::isRequired);
    }

    public CodegenParameter addParameter(String name) {
        var parameter = new CodegenParameter(name);
        parameters.add(parameter);
        return parameter;
    }

    public CodegenContent addRequestBody(String contentType) {
        var content = new CodegenContent(contentType);
        getRequestBodies().put(contentType, content);
        return content;
    }

    public CodegenContent addResponseType(String contentType) {
        var content = new CodegenContent(contentType);
        getResponseTypes().put(contentType, content);
        return content;
    }

    private List<CodegenParameter> filterOnLocation(CodegenParameter.ParameterLocation location) {
        return parameters.stream().filter(codegenParameter -> codegenParameter.getIn().equals(location)).toList();
    }

    private static String camelCaseSlashes(String pathExpression) {
        var result = new StringBuilder();
        var slashPos = 0;
        int oldPos = 1;
        while ((slashPos = pathExpression.indexOf('/', oldPos)) != -1) {
            result.append(pathExpression, oldPos, slashPos);
            result.append(Character.toUpperCase(pathExpression.charAt(slashPos + 1)));
            oldPos = slashPos + 2;
        }
        result.append(pathExpression.substring(oldPos));
        return result.toString();
    }

    private static String inlinePathParams(String s) {
        var result = new StringBuilder();
        var startPos = 0;
        int oldPos1 = 0;
        while ((startPos = s.indexOf('{', oldPos1)) != -1) {
            result.append(s, oldPos1, startPos);
            var endPos = s.indexOf('}', startPos);
            result.append(toUpperCamelCase(s.substring(startPos + 1, endPos)));
            oldPos1 = endPos + 1;
        }
        result.append(s.substring(oldPos1));
        return result.toString();
    }
}

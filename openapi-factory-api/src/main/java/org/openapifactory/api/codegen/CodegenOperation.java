package org.openapifactory.api.codegen;

import lombok.Data;
import lombok.ToString;
import org.openapifactory.api.codegen.types.CodegenAnonymousObjectModel;
import org.openapifactory.api.codegen.types.CodegenType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.openapifactory.api.StringUtil.toUpperCamelCase;

@Data
@ToString(of = {"method", "path", "operationId"})
public class CodegenOperation {
    private final OpenapiSpec spec;
    private final String method, path;
    private String operationId;
    private String summary;
    private final List<CodegenSecurity> security = new ArrayList<>();
    private final List<CodegenResponse> responses = new ArrayList<>();

    public CodegenOperation(OpenapiSpec spec, String method, String path) {
        this.spec = spec;
        this.method = method;
        this.path = path;
        this.operationId = defaultOperationId();
    }

    private String defaultOperationId() {
        return inlinePathParams(camelCaseSlashes(path)) + toUpperCamelCase(method);
    }

    private List<CodegenParameter> parameters = new ArrayList<>();
    private Map<String, CodegenContent> requestBodies = new LinkedHashMap<>();

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
        return requestBodies.getOrDefault("application/json",
                requestBodies.getOrDefault("application/x-www-form-urlencoded",
                        requestBodies.get("multipart/form-data")));
    }

    public boolean hasOnlyOptionalParams() {
        return parameters.stream().noneMatch(CodegenParameter::isRequired)  &&
               requestBodies.values().stream().noneMatch(CodegenContent::isRequired) &&
               security.isEmpty();
    }

    public CodegenParameter addParameter(String name) {
        var parameter = new CodegenParameter(spec, name);
        parameters.add(parameter);
        return parameter;
    }

    public CodegenContent addRequestBody(String contentType) {
        var content = new CodegenContent(spec, contentType);
        getRequestBodies().put(contentType, content);
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

    public CodegenSecurity addSecurity(String name) {
        var security = new CodegenSecurity(name);
        this.security.add(security);
        return security;
    }

    public boolean isGET() {
        return getMethod().equalsIgnoreCase("GET");
    }

    public CodegenType addRequestModel(CodegenAnonymousObjectModel object) {
        var namedModel = spec.addGenericModel(toUpperCamelCase(getOperationId()) + "Request");
        object.getProperties().forEach(namedModel.getProperties()::put);
        return namedModel;
    }

    public CodegenType addResponseModel(CodegenAnonymousObjectModel object, int responseCode) {
        var namedModel = spec.addGenericModel(toUpperCamelCase(getOperationId()) + responseCode + "Response");
        object.getProperties().forEach(namedModel.getProperties()::put);
        return namedModel;
    }

    public CodegenResponse addResponse(int statusCode) {
        var response = new CodegenResponse(spec, statusCode);
        responses.add(response);
        return response;
    }
}

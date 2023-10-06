package org.openapifactory.api.codegen;

import lombok.Data;
import lombok.ToString;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class CodegenResponse {
    @ToString.Exclude
    private final OpenapiSpec spec;
    private final int responseCode;
    private Map<String, CodegenContent> responseTypes = new LinkedHashMap<>();

    public CodegenContent addResponseType(String contentType) {
        var content = new CodegenContent(spec, contentType);
        getResponseTypes().put(contentType.split(";")[0], content);
        return content;
    }

    public CodegenContent getContent() {
        return responseTypes.getOrDefault("application/json",
                responseTypes.get("application/xml"));
    }

    public boolean is2xx() {
        return 200 <= responseCode && responseCode < 300;
    }

    public boolean is4xx() {
        return 400 <= responseCode && responseCode < 500;
    }
}

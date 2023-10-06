package org.openapifactory.typescript.files;

import org.openapifactory.api.FileGenerator;
import org.openapifactory.api.codegen.CodegenApi;
import org.openapifactory.api.codegen.CodegenContent;
import org.openapifactory.api.codegen.CodegenOperation;
import org.openapifactory.api.codegen.CodegenParameter;
import org.openapifactory.api.codegen.CodegenProp;
import org.openapifactory.api.codegen.CodegenSecurity;
import org.openapifactory.api.codegen.OpenapiSpec;
import org.openapifactory.typescript.TypescriptFragments;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.openapifactory.api.StringUtil.indent;
import static org.openapifactory.api.StringUtil.join;
import static org.openapifactory.api.StringUtil.lines;
import static org.openapifactory.api.StringUtil.toLowerCamelCase;
import static org.openapifactory.api.StringUtil.toUpperCamelCase;
import static org.openapifactory.typescript.TypescriptFragments.getName;
import static org.openapifactory.typescript.TypescriptFragments.getRequestTypeName;
import static org.openapifactory.typescript.TypescriptFragments.getTypeName;
import static org.openapifactory.typescript.TypescriptFragments.propertiesDefinition;

public class ApiTsFile implements FileGenerator {

    private final OpenapiSpec spec;

    public ApiTsFile(OpenapiSpec spec) {
        this.spec = spec;
    }

    @Override
    public void generate(Path outputRoot) throws IOException {
        Files.writeString(outputRoot.resolve("api.ts"), content());
    }

    protected String content() {
        return String.join("", List.of(
                eslintSection(),
                TypescriptFragments.documentationSection(spec),
                importSection(),
                apiListSection(),
                apiSection(),
                serverSection(),
                securitySchemeSection()
        ));
    }


    protected String eslintSection() {
        return "/* eslint @typescript-eslint/no-unused-vars: off */\n";
    }

    protected String importSection() {
        return "\n" +
               "import {\n" +
               indent(4, spec.getModels(), m ->
                       m.getName() + ",\n" +
                       (m.hasReadOnlyProperties() ? (m.getName() + "Request,\n") : "")
               ) + "} from \"./model\";\n" +
               "\n" +
               "import { BaseAPI, RequestCallOptions, SecurityScheme } from \"./base\";\n";
    }

    protected String apiListSection() {
        return String.join("\n",
                "",
                "export interface ApplicationApis {",
                lines(spec.getApis(), a
                        -> "    " + toLowerCamelCase(a.getTag()) + "Api: " + toUpperCamelCase(a.getTag()) + "ApiInterface;"
                ),
                "}",
                ""
        );
    }

    protected String apiSection() {
        var result = new ArrayList<String>();
        for (var api : spec.getApis()) {
            result.add(interfaceDefinition(api));
            result.add(apiImplementation(api));
        }
        return String.join("", result);
    }

    private static String interfaceDefinition(CodegenApi api) {
        return "\n" +
               "/**\n" +
               " * " + api.getApiName() + " - object-oriented interface\n" +
               " */\n" +
               "export interface " + api.getApiName() + "Interface {\n" +
               indent(4, api.getOperations(), ApiTsFile::operationDeclaration) +
               "}\n";
    }

    private static String operationDeclaration(CodegenOperation op) {
        var params = operationParameters(op).indent(4);
        return "/**\n" +
               " *\n" +
               (op.getSummary() != null ? " * @summary " + op.getSummary() + "\n" : "") +
               (params.isEmpty() ? "" : " * @param {*} [params] Request parameters, including pathParams, queryParams (including bodyParams) and http options.\n") +
               " * @throws {HttpError}\n" +
               " */\n" +
               op.getOperationId() +
               (params.isEmpty()
                       ? "(params?: RequestCallOptions)" :
                       "(params" + (op.hasOnlyOptionalParams() ? "?" : "") + ": {\n" + params + "} & RequestCallOptions)")
               + ": Promise<" + getResponseType(op.getResponseType()) + ">;\n";
    }

    private static String apiImplementation(CodegenApi api) {
        return ("\n" +
                "/**\n" +
                " * " + api.getApiName() + " - object-oriented interface\n" +
                " */\n" +
                "export class " + api.getApiName() + " extends BaseAPI implements " + api.getApiName() + "Interface {\n" +
                indent(4, api.getOperations(), ApiTsFile::operationImplementation) +
                "}\n");
    }

    private static String operationImplementation(CodegenOperation op) {
        var params = operationParameters(op).indent(4);

        return "/**\n" +
               " *\n" +
               (op.getSummary() != null ? " * @summary " + op.getSummary() + "\n" : "") +
               (params.isEmpty() ? "" : " * @param {*} [params] Request parameters, including pathParams, queryParams (including bodyParams) and http options.\n") +
               " * @throws {HttpError}\n" +
               " */\n" +
               "public async " + op.getOperationId() +
               (params.isEmpty()
                       ? "(params: RequestCallOptions = {})"
                       : "(params" + (op.hasOnlyOptionalParams() ? "?" : "") + ": {\n" + params + "} & RequestCallOptions)") +
               ": Promise<" + getResponseType(op.getResponseType()) + "> {\n" +
               functionBody(op, op.getRequestBody()).indent(4) +
               "}\n";
    }

    private static String operationParameters(CodegenOperation op) {
        var params = "";
        if (!op.getPathParams().isEmpty()) {
            params += paramsDefinition("pathParams", op.getPathParams());
        }
        if (!op.getQueryParams().isEmpty()) {
            params += paramsDefinition("queryParams", op.getQueryParams());
        }
        if (op.getRequestBody() != null) {
            var p = op.getRequestBody();
            params += getName(p) +
                      (p.isRequired() && p.getType().hasNoRequiredProperties() ? "" : "?") +
                      ": " + getRequestTypeName(p.getType()) +
                      ";\n";
        }
        if (!op.getHeaderParams().isEmpty()) {
            params += paramsDefinitionWithQuotes("headers", op.getHeaderParams());
        }
        if (!op.getSecurity().isEmpty()) {
            params += "security: " + join(" | ", op.getSecurity(), CodegenSecurity::getName) + ";\n";
        }
        return params;
    }

    private static String functionBody(CodegenOperation op, CodegenContent requestBody) {
        String fetchExpression;
        if (op.getPathParams().isEmpty() && op.getQueryParams().isEmpty()) {
            fetchExpression = "this.basePath + \"" + op.getPath() + "\"";
        } else {
            var queryOptionsLines = op.getQueryParams().stream()
                    .filter(ApiTsFile::hasQueryOptions)
                    .map(ApiTsFile::getQueryOptions)
                    .collect(Collectors.joining(""));
            var queryOptions = queryOptionsLines.isEmpty() ? "{}" : ("{\n" + queryOptionsLines.indent(8) + "    }");
            fetchExpression =
                    "this.url(\"" + op.getPath() + "\", " +
                    (op.getPathParams().isEmpty() ? "{}" : "params.pathParams") +
                    (op.getQueryParams().isEmpty() ? "" : ", params?.queryParams, " + queryOptions) + ")";
        }
        if (requestBody != null || !op.getSecurity().isEmpty()) {
            return
                    "return await this.fetch(\n" +
                     "    " + fetchExpression + ",\n" +
                     "    {\n" +
                     "        ...params,\n" +
                     (op.isGET() ? "" : "        method: \"" + op.getMethod() + "\",\n") +
                     (requestBody == null ? "" : "        body: " + requestBodyExpression(op, requestBody) + ",\n") +
                     "        headers: {\n" +
                     "            ...this.removeEmpty(params" + (op.hasOnlyOptionalParams() ? "?" : "") + ".headers),\n" +
                     (op.getSecurity().isEmpty() ? "" : "            ...params.security?.headers(),\n") +
                     (requestBody == null ? "" : "            \"Content-Type\": \"" + requestBody.getContentType() + "\",\n") +
                     "        },\n" +
                     "    }\n" +
                     ");\n";
        } else if (op.isGET()) {
            return "return await this.fetch(\n" +
                   "    " + fetchExpression + ", params\n" +
                   ");\n";
        } else {
            return "return await this.fetch(\n" +
                   "    " + fetchExpression + ",\n" +
                   "    {\n" +
                   "        ...params,\n" +
                   "        method: \"" + op.getMethod() + "\",\n" +
                   "    }\n" +
                   ");\n";
        }
    }

    private static String requestBodyExpression(CodegenOperation op, CodegenContent requestBody) {
        return switch (requestBody.getContentType()) {
            case "application/x-www-form-urlencoded" -> "this.formData(params.formParams)";
            case "multipart/form-data" -> "this.formData(params.formParams)";
            case "application/json" ->
                    op.hasOnlyOptionalParams()
                            ? "params?." + getName(requestBody) + " ? JSON.stringify(params." + getName(requestBody) + ") : undefined"
                            : "JSON.stringify(params." + getName(requestBody) + ")";
            default -> throw new RuntimeException();
        };
    }

    private static String paramsDefinition(String paramName, List<CodegenParameter> params) {
        return paramName +
               (params.stream().noneMatch(CodegenParameter::isRequired) ? "?" : "") + ": { " +
               propertiesDefinition(params) +
               ", };\n";
    }

    private static String paramsDefinitionWithQuotes(String paramName, List<CodegenParameter> params) {
        return paramName +
               (params.stream().noneMatch(CodegenParameter::isRequired) ? "?" : "") + ": { " +
               join(", ", params, p ->
                       '"' + getName(p) + '"' + (p.isRequired() ? "" : "?") + ": " + getTypeName(p.getType())) +
               ", };\n";
    }

    private static String getQueryOptions(CodegenParameter p) {
        var options = new ArrayList<String>();
        if (p.getStyle() == CodegenParameter.Style.spaceDelimited) {
            options.add("delimiter: \" \"");
        } else if (p.getStyle() == CodegenParameter.Style.pipeDelimited) {
            options.add("delimiter: \"|\"");
        }
        if (!p.isExplode()) {
            options.add("explode: false");
        }
        if (p.getType().isDate()) {
            options.add("format: \"date\"");
        }
        return p.getName() + ": " + "{ " + String.join(", ", options) + " },\n";
    }

    private static boolean hasQueryOptions(CodegenParameter p) {
        var processedStyles = Set.of(CodegenParameter.Style.pipeDelimited, CodegenParameter.Style.spaceDelimited);
        return !p.isExplode() || p.getType().isDate() || (p.getStyle() != null && processedStyles.contains(p.getStyle()));
    }

    private static String getResponseType(CodegenProp responseType) {
        return responseType == null ? "void" : getTypeName(responseType.getType());
    }

    protected String serverSection() {
        return
                "\n" +
                "type ServerNames =\n" +
                lines(spec.getServers(), s ->
                        "    | " + "\"" + Objects.toString(s.getDescription(), "default") + "\"") + ";\n" +
                "\n" +
                "export const servers: Record<ServerNames, ApplicationApis> = {\n" +
                indent(4, spec.getServers(),
                        s -> {
                            var serverName = s.getDescription() == null ? "default" : "\"" + s.getDescription() + "\"";
                            return serverName + ": {\n" +
                                   indent(4, spec.getApis(), api ->
                                           toLowerCamelCase(api.getApiName()) + ": new " + api.getApiName() + "(\"" + s.getUrl() + "\"),\n") +
                                   "},\n";
                        }
                ) +
                "};\n" +
                "\n";
    }

    protected String securitySchemeSection() {
        return join(spec.getSecuritySchemes(), scheme -> """

                export class %s implements SecurityScheme {
                    constructor(private bearerToken: string) {}
                                    
                    headers(): Record<string, string> {
                        return {
                            "Authorization": `Bearer ${this.bearerToken}`,
                        }
                    }
                }
                """.formatted(scheme.getName()));
    }
}

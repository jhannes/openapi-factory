package org.openapifactory.typescript.files;

import org.openapifactory.api.FileGenerator;
import org.openapifactory.api.codegen.CodegenApi;
import org.openapifactory.api.codegen.CodegenContent;
import org.openapifactory.api.codegen.CodegenOperation;
import org.openapifactory.api.codegen.CodegenParameter;
import org.openapifactory.api.codegen.CodegenProp;
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
import static org.openapifactory.typescript.TypescriptFragments.getTypeName;
import static org.openapifactory.typescript.TypescriptFragments.propertiesDefinition;
import static org.openapifactory.typescript.TypescriptFragments.propertyDefinition;

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
                serverSection()
        ));
    }


    protected String eslintSection() {
        return "/* eslint @typescript-eslint/no-unused-vars: off */\n";
    }

    protected String importSection() {
        var imports = lines(spec.getModels(), m -> "    " + m.getName() + ",");
        return """
                                
                import {
                %s
                } from "./model";
                                
                import { BaseAPI, RequestCallOptions, SecurityScheme } from "./base";
                """.formatted(imports);
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
        var operations = lines(api.getOperations(), ApiTsFile::operationDeclaration);
        return """

                /**
                 * %s - object-oriented interface
                 */
                export interface %s {
                %s
                }
                """.formatted(api.getApiName(), api.getApiName() + "Interface", operations);
    }

    private static String operationDeclaration(CodegenOperation op) {
        var params = "";
        if (!op.getPathParams().isEmpty()) {
            params += paramsDefinition("pathParams", op.getPathParams()).indent(8);
        }
        if (!op.getQueryParams().isEmpty()) {
            params += paramsDefinition("queryParams", op.getQueryParams()).indent(8);
        }
        if (op.getRequestBody() != null) {
            params += (propertyDefinition(op.getRequestBody()) + ";").indent(8);
        }
        if (!op.getHeaderParams().isEmpty()) {
            params += paramsDefinitionWithQuotes("headers", op.getHeaderParams()).indent(8);
        }
        return
                "    /**\n" +
                "     *\n" +
                (params.isEmpty() ? "" : "     * @param {*} [params] Request parameters, including pathParams, queryParams (including bodyParams) and http options.\n") +
                "     * @throws {HttpError}\n" +
                "     */\n" +
                "    " + op.getOperationId() +
                (params.isEmpty()
                        ? "(params?: RequestCallOptions)" :
                        "(params" + (op.hasOnlyOptionalParams() ? "?" : "") + ": {\n" + params + "    } & RequestCallOptions)")
                + ": Promise<" + getResponseType(op.getResponseType()) + ">;";
    }

    private static String apiImplementation(CodegenApi codegenApi) {
        var operations = join(codegenApi.getOperations(), ApiTsFile::operationImplementation);
        return """
                                
                /**
                 * %s - object-oriented interface
                 */
                export class %s extends BaseAPI implements %s {
                %s}
                """.formatted(codegenApi.getApiName(), codegenApi.getApiName(), codegenApi.getApiName() + "Interface", operations);
    }

    private static String operationImplementation(CodegenOperation op) {
        var params = "";
        if (!op.getPathParams().isEmpty()) {
            params += paramsDefinition("pathParams", op.getPathParams()).indent(4);
        }
        if (!op.getQueryParams().isEmpty()) {
            params += paramsDefinition("queryParams", op.getQueryParams()).indent(4);
        }
        var requestBody = op.getRequestBody();
        if (requestBody != null) {
            params += (propertyDefinition(requestBody) + ";").indent(4);
        }
        if (!op.getHeaderParams().isEmpty()) {
            params += paramsDefinitionWithQuotes("headers", op.getHeaderParams()).indent(4);
        }

        return ("/**\n" +
                " *\n" +
                (params.isEmpty() ? "" : " * @param {*} [params] Request parameters, including pathParams, queryParams (including bodyParams) and http options.\n") +
                " * @throws {HttpError}\n" +
                " */\n" +
                "public async " + op.getOperationId() +
                (params.isEmpty() ? "(params: RequestCallOptions = {})" : "(params" + (op.hasOnlyOptionalParams() ? "?" : "") + ": {\n" + params + "} & RequestCallOptions)") +
                ": Promise<" + getResponseType(op.getResponseType()) + "> {\n" +
                functionBody(op, requestBody) +
                "}\n").indent(4);
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
            var queryOptions = queryOptionsLines.isEmpty() ? "{}" : ("{\n" + queryOptionsLines.indent(12) + "        }");
            fetchExpression =
                    "this.url(\"" + op.getPath() + "\", " +
                    (op.getPathParams().isEmpty() ? "{}" : "params.pathParams") +
                    (op.getQueryParams().isEmpty() ? "" : ", params?.queryParams, " + queryOptions) + ")";
        }
        if (requestBody != null) {
            var body = requestBody.isFormContent()
                    ? "this.formData(params.formParams)"
                    : (op.hasOnlyOptionalParams()
                    ? ("params?." + getName(requestBody) + " ? JSON.stringify(params." + getName(requestBody) + ") : undefined")
                    : "JSON.stringify(params." + getName(requestBody) + ")");
            return
                    ("return await this.fetch(\n" +
                     "    " + fetchExpression + ",\n" +
                     "    {\n" +
                     "        ...params,\n" +
                     "        method: \"" + op.getMethod() + "\",\n" +
                     "        body: " + body + ",\n" +
                     "        headers: {\n" +
                     "            ...this.removeEmpty(params" + (op.hasOnlyOptionalParams() ? "?" : "") + ".headers),\n" +
                     "            \"Content-Type\": \"" + requestBody.getContentType() + "\",\n" +
                     "        },\n" +
                     "    }\n" +
                     ");\n").indent(4);
        } else if (op.getMethod().equals("GET")) {
            return """
                        return await this.fetch(
                            %s, params
                        );
                    """.formatted(fetchExpression);
        } else {
            return "OIOI";
        }
    }

    private static String paramsDefinition(String paramName, List<CodegenParameter> params) {
        return paramName +
               (params.stream().noneMatch(CodegenParameter::isRequired) ? "?" : "") + ": { " +
               propertiesDefinition(params) +
               ", };";
    }

    private static String paramsDefinitionWithQuotes(String paramName, List<CodegenParameter> params) {
        return paramName +
               (params.stream().noneMatch(CodegenParameter::isRequired) ? "?" : "") + ": { " +
               join(", ", params, p ->
                       '"' + getName(p) + '"' + (p.isRequired() ? "" : "?") + ": " + getTypeName(p.getType())) +
               ", };";
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
        if (p.isDate()) {
            options.add("format: \"date\"");
        }
        return p.getName() + ": " + "{ " + String.join(", ", options) + " },\n";
    }

    private static boolean hasQueryOptions(CodegenParameter p) {
        var processedStyles = Set.of(CodegenParameter.Style.pipeDelimited, CodegenParameter.Style.spaceDelimited);
        return !p.isExplode() || p.isDate() || (p.getStyle() != null && processedStyles.contains(p.getStyle()));
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
                                   "},";
                        }
                ) +
                "};\n" +
                "\n";

        /*
        return """
                                
                type ServerNames =
                    | "default";
                                
                export const servers: Record<ServerNames, ApplicationApis> = {
                    "default": {
                          discoveryApi: new DiscoveryApi(""),
                          identityClientApi: new IdentityClientApi(""),
                          identityProviderApi: new IdentityProviderApi(""),
                    },
                };
                                
                """;

         */
    }
}

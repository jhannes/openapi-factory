package org.openapifactory.typescript.files;

import org.openapifactory.api.FileGenerator;
import org.openapifactory.api.codegen.CodegenApi;
import org.openapifactory.api.codegen.CodegenContent;
import org.openapifactory.api.codegen.CodegenOperation;
import org.openapifactory.api.codegen.CodegenParameter;
import org.openapifactory.api.codegen.CodegenResponse;
import org.openapifactory.api.codegen.CodegenSecurity;
import org.openapifactory.api.codegen.CodegenServer;
import org.openapifactory.api.codegen.OpenapiSpec;
import org.openapifactory.api.codegen.types.CodegenSchema;
import org.openapifactory.typescript.TypescriptFragments;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.openapifactory.api.StringUtil.indent;
import static org.openapifactory.api.StringUtil.join;
import static org.openapifactory.api.StringUtil.lines;
import static org.openapifactory.api.StringUtil.toLowerCamelCase;
import static org.openapifactory.typescript.TypescriptFragments.getApiName;
import static org.openapifactory.typescript.TypescriptFragments.getPropName;
import static org.openapifactory.typescript.TypescriptFragments.getRequestTypeName;
import static org.openapifactory.typescript.TypescriptFragments.getResponseTypeName;
import static org.openapifactory.typescript.TypescriptFragments.getTypeName;

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
        return STR."""
                /* eslint @typescript-eslint/no-unused-vars: off */
                \{TypescriptFragments.documentationSection(spec)}
                import {
                \{ indent(4, getModels(), m -> m + ",\n") }} from "./model";

                import { BaseAPI, RequestCallOptions, SecurityScheme } from "./base";

                export interface ApplicationApis {
                \{ lines(spec.getApis(), a -> "    " + toLowerCamelCase(a.getTag()) + "Api: " + getApiName(a) + "Interface;") }
                }
                \{join(spec.getApis(), api ->
                        interfaceDefinition(api) +
                        apiImplementation(api)
                )}
                type ServerNames =
                \{ lines(spec.getServers(), s -> "    | " + "\"" + Objects.toString(s.getDescription(), "default") + "\"") };

                export const servers: Record<ServerNames, ApplicationApis> = {
                \{ indent(4, spec.getServers(), server ->
                    (server.getDescription() == null ? "default" : "\"" + server.getDescription() + "\"") + ": {\n" +
                       indent(4, spec.getApis(), api -> apiInstansiation(server, api)) + "},\n")
                    }};

                \{join(spec.getSecuritySchemes(), scheme ->
                        STR. """

                        export class \{ scheme.getKey() } implements SecurityScheme {
                            constructor(private bearerToken: string) {}

                            headers(): Record<string, string> {
                                return {
                                    "Authorization": `Bearer ${this.bearerToken}`,
                                }
                            }
                        }
                        """
                )}""";
    }


    private TreeSet<String> getModels() {
        var models = new TreeSet<String>();
        spec.getModels().stream().map(TypescriptFragments::getTypeName).forEach(models::add);
        spec.getModels().stream()
                .filter(CodegenSchema::hasWriteOnlyProperties)
                .map(TypescriptFragments::getResponseTypeName)
                .forEach(models::add);
        spec.getModels().stream()
                .filter(CodegenSchema::hasReadOnlyProperties)
                .map(TypescriptFragments::getRequestTypeName)
                .forEach(models::add);
        return models;
    }

    private static String interfaceDefinition(CodegenApi api) {
        return STR. """

                /**
                 * \{ getApiName(api) } - object-oriented interface
                 */
                export interface \{ getApiName(api) }Interface {
                \{ indent(4, api.getOperations(), ApiTsFile::operationDeclaration) }}
                """ ;
    }

    private static String operationDeclaration(CodegenOperation op) {
        return STR."""
            /**
             *
            \{
                (Optional.ofNullable(op.getSummary()).map(s -> " * @summary " + s + "\n").orElse(""))
            }\{
                (op.hasParams() ? " * @param {*} [params] Request parameters, including pathParams, queryParams (including bodyParams) and http options.\n" : "")
            } * @throws {HttpError}
             */
            \{op.getOperationId()}\{
                op.hasParams()
                        ? "(params" + (op.hasOnlyOptionalParams() ? "?" : "") + ": {\n" + operationParameters(op).indent(4) + "} & RequestCallOptions)"
                        : "(params?: RequestCallOptions)"
            }: Promise<\{ getResponseType(op) }>;
            """;
    }

    private static String apiImplementation(CodegenApi api) {
        return STR."""

                /**
                 * \{getApiName(api)} - object-oriented interface
                 */
                export class \{getApiName(api)} extends BaseAPI implements \{getApiName(api)}Interface {
                \{indent(4, api.getOperations(), ApiTsFile::operationImplementation)}}
                """;
    }

    private static String operationImplementation(CodegenOperation op) {
        return STR."""
               /**
                *
               \{(op.getSummary() != null ? " * @summary " + op.getSummary() + "\n" : "")
                }\{(op.hasParams() ? " * @param {*} [params] Request parameters, including pathParams, queryParams (including bodyParams) and http options.\n" : "")
                } * @throws {HttpError}
                */
               public async \{op.getOperationId()}\{
                (op.hasParams()
                        ? "(params" + (op.hasOnlyOptionalParams() ? "?" : "") + ": {\n" + operationParameters(op).indent(4) + "} & RequestCallOptions)"
                        : "(params: RequestCallOptions = {})")
                }: Promise<\{getResponseType(op)}> {
               \{functionBody(op, op.getRequestBody()).indent(4)}}
               """;
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
            var propName = p.isFormContent() ? "formParams" : TypescriptFragments.variableName(p.getSchema());
            params += propName +
                      // Commented out for backwards compatibility
                      //(p.isRequired() && p.getSchema().hasNoRequiredProperties() ? "" : "?") +
                      ((p.isRequired() || p.isFormContent()) ? "" : "?") +
                      ": " + getRequestTypeName(p.getSchema()) +
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
        if (requestBody != null || !op.getSecurity().isEmpty()) {
            return STR."""
                   return await this.fetch(
                       \{getFetchExpression(op)},
                       {
                           ...params,
                   \{(op.isGET() ? "" : "        method: \"" + op.getMethod().toUpperCase() + "\",\n")
                   }\{(requestBody == null ? "" : "        body: " + requestBodyExpression(op, requestBody) + ",\n")
                   }        headers: {
                               ...this.removeEmpty(params\{op.hasOnlyOptionalParams() ? "?" : ""}.headers),
                   \{(op.getSecurity().isEmpty() ? "" : "            ...params.security?.headers(),\n")
                   }\{(requestBody == null ? "" : "            \"Content-Type\": \"" + requestBody.getContentType() + "\",\n")
                   }        },
                       }
                   );
                   """;
        } else if (op.isGET()) {
            return STR."""
                   return await this.fetch(
                       \{getFetchExpression(op)}, params
                   );
                   """;
        } else {
            return STR."""
                   return await this.fetch(
                       \{getFetchExpression(op)},
                       {
                           ...params,
                           method: "\{op.getMethod().toUpperCase()}",
                       }
                   );
                   """;
        }
    }

    private static String getFetchExpression(CodegenOperation op) {
        if (op.getPathParams().isEmpty() && op.getQueryParams().isEmpty()) {
            return "this.basePath + \"" + op.getPath() + "\"";
        } else {
            var queryOptionsLines = op.getQueryParams().stream()
                    .filter(ApiTsFile::hasQueryOptions)
                    .map(ApiTsFile::getQueryOptions)
                    .collect(Collectors.joining(""));
            var queryOptions = queryOptionsLines.isEmpty() ? "{}" : ("{\n" + queryOptionsLines.indent(8) + "    }");
            return        "this.url(\"" + op.getPath() + "\", " +
                    (op.getPathParams().isEmpty() ? "{}" : "params.pathParams") +
                    (op.getQueryParams().isEmpty() ? "" : ", params?.queryParams, " + queryOptions) + ")";
        }
    }

    private static String requestBodyExpression(CodegenOperation op, CodegenContent requestBody) {
        var propName = TypescriptFragments.variableName(requestBody.getSchema());
        if (requestBody.isFormContent()) {
            return "this.formData(params.formParams)";
        } else if (op.hasOnlyOptionalParams()) {
            return "params?." + propName + " ? JSON.stringify(params." + propName + ") : undefined";
        } else {
            return "JSON.stringify(params." + propName + ")";
        }
    }

    private static String paramsDefinition(String paramName, List<CodegenParameter> params) {
        return paramName +
               (params.stream().noneMatch(CodegenParameter::isRequired) ? "?" : "") + ": { " +
               join("; ", params, TypescriptFragments::propertyDefinition) +
               " };\n";
    }

    private static String paramsDefinitionWithQuotes(String paramName, List<CodegenParameter> params) {
        return paramName +
               (params.stream().noneMatch(CodegenParameter::isRequired) ? "?" : "") + ": { " +
               join("; ", params, p ->
                       '"' + getPropName(p) + '"' + (p.isRequired() ? "" : "?") + ": " + getTypeName(p.getSchema())) +
               " };\n";
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
        if (p.getSchema().isDate()) {
            options.add("format: \"date\"");
        }
        return p.getName() + ": " + "{ " + String.join(", ", options) + " },\n";
    }

    private static boolean hasQueryOptions(CodegenParameter p) {
        var processedStyles = Set.of(CodegenParameter.Style.pipeDelimited, CodegenParameter.Style.spaceDelimited);
        return !p.isExplode() || p.getSchema().isDate() || (p.getStyle() != null && processedStyles.contains(p.getStyle()));
    }

    private static String getResponseType(CodegenOperation operation) {
        if (operation.getResponses().stream().allMatch(r -> r.getResponseTypes().isEmpty() || r.is4xx())) {
            return "void";
        }
        return operation.getResponses().stream()
                .filter(CodegenResponse::is2xx)
                .map(o -> o.getContent() == null ? "undefined" : (getResponseTypeName(o.getContent().getSchema())))
                .collect(Collectors.joining("|"));
    }

    private static String apiInstansiation(CodegenServer s, CodegenApi api) {
        return toLowerCamelCase(api.getTag() + "Api") + ": new " + getApiName(api) + "(\"" + s.getUrl() + "\"),\n";
    }

}

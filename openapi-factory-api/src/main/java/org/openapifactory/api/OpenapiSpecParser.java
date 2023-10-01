package org.openapifactory.api;

import org.openapifactory.api.codegen.CodegenArrayType;
import org.openapifactory.api.codegen.CodegenContact;
import org.openapifactory.api.codegen.CodegenContent;
import org.openapifactory.api.codegen.CodegenEnumModel;
import org.openapifactory.api.codegen.CodegenGenericModel;
import org.openapifactory.api.codegen.CodegenInlineEnumType;
import org.openapifactory.api.codegen.CodegenInlineObjectType;
import org.openapifactory.api.codegen.CodegenModel;
import org.openapifactory.api.codegen.CodegenOneOfModel;
import org.openapifactory.api.codegen.CodegenOperation;
import org.openapifactory.api.codegen.CodegenParameter;
import org.openapifactory.api.codegen.CodegenPrimitiveType;
import org.openapifactory.api.codegen.CodegenProp;
import org.openapifactory.api.codegen.CodegenProperty;
import org.openapifactory.api.codegen.CodegenServer;
import org.openapifactory.api.codegen.CodegenType;
import org.openapifactory.api.codegen.CodegenTypeRef;
import org.openapifactory.api.codegen.OpenapiSpec;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.composer.Composer;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.parser.ParserImpl;
import org.yaml.snakeyaml.reader.StreamReader;
import org.yaml.snakeyaml.resolver.Resolver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static org.openapifactory.api.StringUtil.toUpperCamelCase;

public class OpenapiSpecParser {

    public OpenapiSpec createOpenApiSpec(Path apiDocument) throws IOException {
        var yaml = new Yaml();
        Map<String, Map<String, ?>> node;
        try (var reader = Files.newBufferedReader(apiDocument)) {
            node = yaml.load(reader);
        }
        Node node2;
        try (var reader = Files.newBufferedReader(apiDocument)) {
            Composer composer =
                    new Composer(new ParserImpl(new StreamReader(reader), new LoaderOptions()), new Resolver(), new LoaderOptions());

            node2 = composer.getSingleNode();
        }
        return createSpec(node);
    }

    protected OpenapiSpec createSpec(Map<String, Map<String, ?>> node) {
        var spec = new OpenapiSpec();
        readSpec(node, spec);
        return spec;
    }

    protected void readSpec(Map<String, Map<String, ?>> node, OpenapiSpec spec) {
        readInfo(spec, node.get("info"));
        readServers(spec, (List<Map<String, ?>>)node.get("servers"));
        readModels(spec, node);
        readPaths(spec, node.get("paths"));
    }

    protected void readServers(OpenapiSpec spec, List<Map<String, ?>> servers) {
        if (servers != null) {
            for (var server : servers) {
                var codegenServer = new CodegenServer();
                codegenServer.setDescription(Objects.toString(server.get("description"), null));
                codegenServer.setUrl(Objects.toString(server.get("url"), null));
                spec.getServers().add(codegenServer);
            }
        } else {
            spec.getServers().add(new CodegenServer());
        }
    }

    private void readModels(OpenapiSpec spec, Map<String, Map<String, ?>> node) {
        var components = (Map<String, Map<String, ?>>) node.get("components");
        var schemas = (Map<String, Map<String, ?>>) components.get("schemas");
        for (var modelName : schemas.keySet()) {
            var model = createModel(modelName, schemas.get(modelName));
            spec.getModelMap().put(modelName, model);
        }
    }

    private CodegenModel createModel(String modelName, Map<String, ?> node) {
        if (node.containsKey("properties")) {
            var generic = new CodegenGenericModel();
            readGenericModel(generic, modelName, node);
            return generic;
        } else if (node.containsKey("enum")) {
            var enumModel = new CodegenEnumModel();
            readEnumModel(enumModel, modelName, node);
            return enumModel;
        } else if (node.containsKey("oneOf")) {
            var oneOf = new CodegenOneOfModel();
            oneOf.setName(modelName + "Dto");
            var discriminatorNode = (Map<String, ?>)node.get("discriminator");
            oneOf.getDiscriminator().setPropertyName(Objects.toString(discriminatorNode.get("propertyName"), null));
            var mapping = (Map<String, String>) discriminatorNode.get("mapping");
            for (var mappingEntry : mapping.entrySet()) {
                oneOf.getDiscriminator().getMapping().put(
                        mappingEntry.getKey(),
                        new CodegenTypeRef(mappingEntry.getValue())
                );
            }
            return oneOf;
        } else {
            throw new IllegalArgumentException("Unsupported model " + modelName + ": " + node);
        }
    }

    private void readEnumModel(CodegenEnumModel enumModel, String modelName, Map<String, ?> node) {
        enumModel.setName(modelName + "Dto");
        enumModel.getValues().addAll((List<String>)node.get("enum"));
    }

    private void readGenericModel(CodegenGenericModel generic, String modelName, Map<String, ?> node) {
        generic.setName(modelName + "Dto");
        generic.setDescription(Objects.toString(node.get("description"), null));
        readProperties(node, generic.getProperties(), generic);
    }

    private static void readProperties(Map<String, ?> node, Map<String, CodegenProperty> modelProperties, CodegenModel model) {
        var required = getStringHashSet(node, "required");
        var properties = (Map<String, Map<String, ?>>) node.get("properties");
        for (var name : properties.keySet()) {
            var codegen = new CodegenProperty();
            codegen.setName(name);
            codegen.setRequired(required.contains(name));
            codegen.setDescription(Objects.toString(properties.get(name).get("description"), null));
            codegen.setExample(Objects.toString(properties.get(name).get("example"), null));
            codegen.setType(getType(properties.get(name), model, codegen));
            modelProperties.put(name, codegen);
        }
    }

    private static Set<String> getStringHashSet(Map<String, ?> node, String key) {
        if (node.containsKey(key)) {
            return new HashSet<>(((List<String>) node.get(key)));
        } else {
            return Set.of();
        }
    }

    protected void readPaths(OpenapiSpec spec, Map<String, ?> paths) {
        for (var pathEntry : paths.entrySet()) {
            var pathExpression = pathEntry.getKey();
            var pathObject = (Map<String, Map<String, ?>>)pathEntry.getValue();
            for (var opEntry : pathObject.entrySet()) {
                var method = opEntry.getKey();
                var operation = (Map<String, ?>) opEntry.getValue();
                var tags = operation.containsKey("tags") ? (List<String>) operation.get("tags") : List.of("default");

                var codegenOperation = createCodegenOperation(
                        pathExpression, method, operation
                );
                for (var tag : tags) {
                    spec.addOperation(tag, codegenOperation);
                }
            }
        }
    }

    private static CodegenOperation createCodegenOperation(String pathExpression, String method, Map<String, ?> operation) {
        var codegen = new CodegenOperation();
        readCodegenOperation(pathExpression, method, operation, codegen);
        return codegen;
    }

    private static void readCodegenOperation(String pathExpression, String method, Map<String, ?> operation, CodegenOperation codegen) {
        codegen.setPath(pathExpression);
        codegen.setMethod(method.toUpperCase());
        var defaultOperationId = getDefaultOperationId(pathExpression, method);
        codegen.setOperationId(Objects.toString(operation.get("operationId"), defaultOperationId));

        var parameters = (List<Map<String, ?>>) operation.get("parameters");
        if (parameters == null) {
            parameters = List.of();
        }
        for (var parameter : parameters) {
            var codegenParameter = new CodegenParameter();
            codegenParameter.setName((String) parameter.get("name"));
            codegenParameter.setRequired(getBoolean(parameter.get("required"), false));
            codegenParameter.setIn(getEnum(parameter.get("in"), CodegenParameter.ParameterLocation.class));
            codegenParameter.setExplode(getBoolean(parameter.get("explode"), true));
            codegenParameter.setStyle(getEnum(parameter.get("style"), CodegenParameter.Style.class));
            codegenParameter.setType(getType((Map<String, ?>) parameter.get("schema"), null, codegenParameter));
            codegen.getParameters().add(codegenParameter);
        }

        if (operation.containsKey("requestBody")) {
            var requestBody = (Map<String, ?>) operation.get("requestBody");
            var content = (Map<String, ?>) requestBody.get("content");
            for (var contentType : content.keySet()) {
                var codegenContent = new CodegenContent();
                codegenContent.setRequired(getBoolean(requestBody.get("required"), false));
                codegenContent.setContentType(contentType);
                codegenContent.setType(getType((Map<String, ?>) ((Map<String, ?>) content.get(contentType)).get("schema"), null, null));
                codegen.getRequestBodies().put(contentType, codegenContent);
            }
        }
        var responses = (Map<?, Map<String, ?>>)operation.getOrDefault("responses", null);
        if (responses != null) {
            for (var o : responses.keySet()) {
                if (o.toString().equals("200")) {
                    var content = (Map<String, Map<String, ?>>)responses.get(o).get("content");
                    if (content != null) {
                        for (var contentType : content.keySet()) {
                            var codegenContent = new CodegenContent();
                            codegenContent.setContentType(contentType);
                            codegenContent.setType(getType((Map<String, ?>) content.get(contentType).get("schema"), null, null));
                            codegen.getResponseTypes().put(contentType, codegenContent);
                        }
                    }
                }
            }
        }

    }

    private static String getDefaultOperationId(String pathExpression, String method) {
        return inlinePathParams(camelCaseSlashes(pathExpression)) + toUpperCamelCase(method);
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

    private static <T extends Enum<T>> T getEnum(Object value, Class<T> enumClass) {
        return value == null ? null : Enum.valueOf(enumClass, value.toString());
    }

    private static CodegenType getType(Map<String, ?> schema, CodegenModel model, CodegenProp prop) {
        var $ref = (String) schema.get("$ref");

        if ($ref != null) {
            return new CodegenTypeRef($ref);
        }
        var type = Objects.toString(schema.get("type"), "string");
        if ("array".equals(type)) {
            var result = new CodegenArrayType();
            result.setUniqueItems(getBoolean(schema.get("uniqueItems"), false));
            result.setItems(getType((Map<String, ?>) schema.get("items"), model, prop));
            return result;
        } else if (schema.containsKey("enum")) {
            var result = new CodegenInlineEnumType();
            result.setType(type);
            result.getValues().addAll((List<String>) schema.get("enum"));
            result.setDeclaredType(model.getName());
            result.setDeclaredProperty(prop.getName());
            return result;
        } else if (schema.containsKey("properties")) {
            var result = new CodegenInlineObjectType();
            result.setDeclaredType(null);
            result.setDeclaredProperty(null);
            readProperties(schema, result.getProperties(), null);
            return result;
        } else {
            var result = new CodegenPrimitiveType();
            result.setType(type);
            result.setFormat(Objects.toString(schema.get("format"), null));
            return result;
        }
    }

    private static boolean getBoolean(Object b, boolean defaultValue) {
        return "true".equals(Objects.toString(b, String.valueOf(defaultValue)));
    }

    private static void readInfo(OpenapiSpec spec, Map<String, ?> infoNode) {
        spec.setTitle(infoNode.get("title").toString());
        spec.setDescription(infoNode.get("description").toString());
        spec.setVersion(infoNode.get("version").toString());
        var contactNode = (Map<String, ?>)infoNode.get("contact");
        if (contactNode != null) {
            var contact = new CodegenContact();
            contact.setName(Objects.toString(contactNode.get("name")));
            contact.setEmail(Objects.toString(contactNode.get("email")));
            spec.setContact(Optional.of(contact));
        }
    }
}

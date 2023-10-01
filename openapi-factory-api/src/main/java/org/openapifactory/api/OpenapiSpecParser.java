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
import org.openapifactory.api.yaml.YamlMappingNode;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static org.openapifactory.api.StringUtil.toUpperCamelCase;

public class OpenapiSpecParser {

    public OpenapiSpec createOpenApiSpec(Path apiDocument) throws IOException {
        return createSpec(YamlMappingNode.read(apiDocument));
    }

    protected OpenapiSpec createSpec(SpecMappingNode node) {
        var spec = new OpenapiSpec();
        readSpec(node, spec);
        return spec;
    }

    protected void readSpec(SpecMappingNode node, OpenapiSpec spec) {
        readInfo(spec, node.mappingNode("info").required());
        readServers(spec, node.sequenceNode("servers"));
        readModels(spec, node);
        readPaths(spec, node.mappingNode("paths").required());
    }

    protected void readServers(OpenapiSpec spec, Maybe<SpecSequenceNode> servers) {
        if (servers.isPresent()) {
            for (var server : servers.required().mappingNodes()) {
                var codegenServer = new CodegenServer();
                codegenServer.setDescription(server.string("description").orNull());
                codegenServer.setUrl(server.string("url").orNull());
                spec.getServers().add(codegenServer);
            }
        } else {
            spec.getServers().add(new CodegenServer());
        }
    }

    private void readModels(OpenapiSpec spec, SpecMappingNode node) {
        var schemas = node.mappingNode("components").required().mappingNode("schemas").required();
        for (var modelName : schemas.keySet()) {
            var model = createModel(modelName, schemas.mappingNode(modelName).required());
            spec.getModelMap().put(modelName, model);
        }
    }

    private CodegenModel createModel(String modelName, SpecMappingNode node) {
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
            var discriminatorNode = node.mappingNode("discriminator").required();
            oneOf.getDiscriminator().setPropertyName(discriminatorNode.string("propertyName").orNull());
            var mapping = discriminatorNode.mappingNode("mapping").required();
            for (var mappingKey : mapping.keySet()) {
                oneOf.getDiscriminator().getMapping().put(
                        mappingKey,
                        new CodegenTypeRef(mapping.string(mappingKey).required())
                );
            }
            return oneOf;
        } else {
            throw new IllegalArgumentException("Unsupported model " + modelName + ": " + node);
        }
    }

    private void readEnumModel(CodegenEnumModel enumModel, String modelName, SpecMappingNode node) {
        enumModel.setName(modelName + "Dto");
        enumModel.getValues().addAll(node.sequenceNode("enum").required().stringList());
    }

    private void readGenericModel(CodegenGenericModel generic, String modelName, SpecMappingNode node) {
        generic.setName(modelName + "Dto");
        generic.setDescription(node.string("description").orNull());
        readProperties(node, generic.getProperties(), generic);
    }

    private static void readProperties(SpecMappingNode node, Map<String, CodegenProperty> modelProperties, CodegenModel model) {
        var required = node.sequenceNode("required")
                .map(SpecSequenceNode::stringList)
                .orElse(Set.of());
        var properties = node.mappingNode("properties").required();
        for (var name : properties.keySet()) {
            var codegen = new CodegenProperty();
            codegen.setName(name);
            codegen.setRequired(required.contains(name));
            var propNode = properties.mappingNode(name).required();
            codegen.setDescription(propNode.string("description").orNull());
            codegen.setExample(propNode.string("example").orNull());
            codegen.setType(getType(propNode, model, codegen));
            modelProperties.put(name, codegen);
        }
    }

    protected void readPaths(OpenapiSpec spec, SpecMappingNode paths) {
        for (var pathExpression : paths.keySet()) {
            var pathNode = paths.mappingNode(pathExpression).required();
            for (var method : pathNode.keySet()) {
                var operation = pathNode.mappingNode(method).required();
                var tags = operation.sequenceNode("tags")
                        .map(SpecSequenceNode::stringList)
                        .orElse(List.of("default"));

                var codegenOperation = createCodegenOperation(
                        pathExpression, method, operation
                );
                for (var tag : tags) {
                    spec.addOperation(tag, codegenOperation);
                }
            }
        }
    }

    private static CodegenOperation createCodegenOperation(String pathExpression, String method, SpecMappingNode operation) {
        var codegen = new CodegenOperation();
        readCodegenOperation(pathExpression, method, operation, codegen);
        return codegen;
    }

    private static void readCodegenOperation(String pathExpression, String method, SpecMappingNode operation, CodegenOperation codegen) {
        codegen.setPath(pathExpression);
        codegen.setMethod(method.toUpperCase());
        var defaultOperationId = getDefaultOperationId(pathExpression, method);
        codegen.setOperationId(operation.string("operationId").orElse(defaultOperationId));

        var parameters = operation.sequenceNode("parameters");
        if (parameters.isPresent()) {
            for (var parameter : parameters.required().mappingNodes()) {
                var codegenParameter = new CodegenParameter();
                codegenParameter.setName(parameter.string("name").required());
                codegenParameter.setRequired(parameter.getBoolean("required").orElse(false));
                codegenParameter.setIn(parameter.getEnum("in", CodegenParameter.ParameterLocation.class).required());
                codegenParameter.setExplode(parameter.getBoolean("explode").orElse(true));
                codegenParameter.setStyle(parameter.getEnum("style", CodegenParameter.Style.class).orNull());
                codegenParameter.setType(getType(parameter.mappingNode("schema").required(), null, codegenParameter));
                codegen.getParameters().add(codegenParameter);
            }
        }

        if (operation.containsKey("requestBody")) {
            var requestBody = operation.mappingNode("requestBody").required();
            var content = requestBody.mappingNode("content").required();
            for (var contentType : content.keySet()) {
                var codegenContent = new CodegenContent();
                codegenContent.setRequired(requestBody.getBoolean("required").orElse(false));
                codegenContent.setContentType(contentType);
                codegenContent.setType(getType((content.mappingNode(contentType).required()).mappingNode("schema").required(), null, null));
                codegen.getRequestBodies().put(contentType, codegenContent);
            }
        }
        if (operation.containsKey("responses")) {
            var responses = operation.mappingNode("responses").required();
            for (var o : responses.keySet()) {
                if (o.equals("200")) {
                    var maybeContent = responses.mappingNode(o).required().mappingNode("content");
                    if (maybeContent.isPresent()) {
                        var content = maybeContent.required();
                        for (var contentType : content.keySet()) {
                            var codegenContent = new CodegenContent();
                            codegenContent.setContentType(contentType);
                            var schema = content.mappingNode(contentType).required().mappingNode("schema").required();
                            codegenContent.setType(getType(schema, null, null));
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

    private static CodegenType getType(SpecMappingNode schema, CodegenModel model, CodegenProp prop) {
        var $ref = schema.string("$ref");

        if ($ref.isPresent()) {
            return new CodegenTypeRef($ref.required());
        }
        var type = schema.string("type").orElse("string");
        if ("array".equals(type)) {
            var result = new CodegenArrayType();
            result.setUniqueItems(schema.getBoolean("uniqueItems").orElse(false));
            result.setItems(getType(schema.mappingNode("items").required(), model, prop));
            return result;
        } else if (schema.containsKey("enum")) {
            var result = new CodegenInlineEnumType();
            result.setType(type);
            result.getValues().addAll(schema.sequenceNode("enum").required().stringList());
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
            result.setFormat(schema.string("format").orNull());
            return result;
        }
    }

    private static void readInfo(OpenapiSpec spec, SpecMappingNode infoNode) {
        spec.setTitle(infoNode.string("title").orNull());
        spec.setDescription(infoNode.string("description").orNull());
        spec.setVersion(infoNode.string("version").orNull());
        infoNode.mappingNode("contact").ifPresent(contactNode -> {
                    var contact = new CodegenContact();
                    contact.setName(Objects.toString(contactNode.string("name").orNull()));
                    contact.setEmail(Objects.toString(contactNode.string("email").orNull()));
                    spec.setContact(Optional.of(contact));
                }
        );
    }
}

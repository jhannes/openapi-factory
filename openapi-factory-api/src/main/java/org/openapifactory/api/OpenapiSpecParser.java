package org.openapifactory.api;

import org.openapifactory.api.codegen.CodegenArrayType;
import org.openapifactory.api.codegen.CodegenConstantType;
import org.openapifactory.api.codegen.CodegenContact;
import org.openapifactory.api.codegen.CodegenInlineEnumType;
import org.openapifactory.api.codegen.CodegenInlineObjectType;
import org.openapifactory.api.codegen.CodegenOperation;
import org.openapifactory.api.codegen.CodegenParameter;
import org.openapifactory.api.codegen.CodegenPrimitiveType;
import org.openapifactory.api.codegen.CodegenProp;
import org.openapifactory.api.codegen.CodegenPropertyMap;
import org.openapifactory.api.codegen.CodegenRecordType;
import org.openapifactory.api.codegen.CodegenType;
import org.openapifactory.api.codegen.CodegenTypeRef;
import org.openapifactory.api.codegen.OpenapiSpec;
import org.openapifactory.api.yaml.YamlMappingNode;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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
        readSecuritySchemes(spec, node);
    }

    protected void readServers(OpenapiSpec spec, Maybe<SpecSequenceNode> servers) {
        if (servers.isPresent()) {
            for (var server : servers.required().mappingNodes()) {
                var codegenServer = spec.addServer();
                codegenServer.setDescription(server.string("description").orNull());
                codegenServer.setUrl(server.string("url").orNull());
            }
        } else {
            spec.addServer();
        }
    }

    private void readModels(OpenapiSpec spec, SpecMappingNode node) {
        var schemas = node.mappingNode("components").required().mappingNode("schemas").required();
        for (var modelName : schemas.keySet()) {
            createModel(spec, modelName, schemas.mappingNode(modelName).required());
        }
    }

    private void createModel(OpenapiSpec spec, String modelName, SpecMappingNode node) {
        if (node.containsKey("properties")) {
            var generic = spec.addGenericModel(modelName);
            generic.setDescription(node.string("description").orNull());
            readProperties(node, generic);
        } else if (node.containsKey("enum")) {
            var enumModel = spec.addEnumModel(modelName);
            enumModel.getValues().addAll(node.sequenceNode("enum").required().stringList());
            node.string("description").ifPresent(enumModel::setDescription);
        } else if (node.containsKey("allOf")) {
            var allOf = spec.addAllOfModel(modelName);
            allOf.getRequired().addAll(node.sequenceNode("required")
                    .map(SpecSequenceNode::stringList)
                    .orElse(List.of()));
            for (var superModel : node.sequenceNode("allOf").required().mappingNodes()) {
                if (superModel.containsKey("$ref")) {
                    allOf.addRefSuperModel(superModel.string("$ref").required());
                } else if (superModel.containsKey("properties")) {
                    readProperties(superModel, allOf.addSuperModel());
                }
            }
        } else if (node.containsKey("oneOf")) {
            var oneOf = spec.addOneOfModel(modelName);
            for (var oneOfNode : node.sequenceNode("oneOf").required().mappingNodes()) {
                oneOf.addOneOf(oneOfNode.string("$ref").required());
            }
            if (node.containsKey("discriminator")) {
                var discriminatorNode = node.mappingNode("discriminator").required();
                oneOf.getDiscriminator().setPropertyName(discriminatorNode.string("propertyName").orNull());
                if (discriminatorNode.containsKey("mapping")) {
                    var mapping = discriminatorNode.mappingNode("mapping").required();
                    for (var mappingKey : mapping.keySet()) {
                        oneOf.addMapping(mappingKey, mapping.string(mappingKey).required());
                    }
                }
            }
        } else {
            throw new IllegalArgumentException("Unsupported model " + modelName + ": " + node);
        }
    }

    private static void readProperties(SpecMappingNode node, CodegenPropertyMap model) {
        var required = node.sequenceNode("required")
                .map(SpecSequenceNode::stringList)
                .orElse(List.of());
        var properties = node.mappingNode("properties").required();
        for (var name : properties.keySet()) {
            var codegen = model.addProperty(name);
            if (required.contains(name)) {
                codegen.setRequired(true);
            }
            var propNode = properties.mappingNode(name).required();
            codegen.setDescription(propNode.string("description").orNull());
            codegen.setExample(propNode.string("example").orNull());
            codegen.setType(getType(propNode, model, codegen));
        }
    }

    protected void readPaths(OpenapiSpec spec, SpecMappingNode paths) {
        for (var pathExpression : paths.keySet()) {
            var pathNode = paths.mappingNode(pathExpression).required();
            for (var method : pathNode.keySet()) {
                var operationNode = pathNode.mappingNode(method).required();
                var tags = operationNode.sequenceNode("tags")
                        .map(SpecSequenceNode::stringList)
                        .orElse(List.of("default"));

                var operation = spec.createOperation(method, pathExpression);
                readCodegenOperation(operationNode, operation);
                for (var tag : tags) {
                    spec.addOperation(tag, operation);
                }
            }
        }
    }

    private void readCodegenOperation(SpecMappingNode operationNode, CodegenOperation operation) {
        operationNode.string("operationId").ifPresent(operation::setOperationId);
        operationNode.string("summary").ifPresent(operation::setSummary);
        var parameters = operationNode.sequenceNode("parameters");
        if (parameters.isPresent()) {
            for (var parameter : parameters.required().mappingNodes()) {
                var codegenParameter = operation.addParameter(parameter.string("name").required());
                codegenParameter.setRequired(parameter.getBoolean("required").orElse(false));
                codegenParameter.setIn(parameter.getEnum("in", CodegenParameter.ParameterLocation.class).required());
                codegenParameter.setExplode(parameter.getBoolean("explode").orElse(true));
                codegenParameter.setStyle(parameter.getEnum("style", CodegenParameter.Style.class).orNull());
                codegenParameter.setType(getType(parameter.mappingNode("schema").required(), null, codegenParameter));
            }
        }

        if (operationNode.containsKey("requestBody")) {
            var requestBody = operationNode.mappingNode("requestBody").required();
            var content = requestBody.mappingNode("content").required();
            for (var contentType : content.keySet()) {
                var codegenContent = operation.addRequestBody(contentType);
                codegenContent.setRequired(requestBody.getBoolean("required").orElse(false));
                codegenContent.setType(getType((content.mappingNode(contentType).required()).mappingNode("schema").required(), null, null));
            }
        }
        if (operationNode.containsKey("responses")) {
            var responses = operationNode.mappingNode("responses").required();
            for (var o : responses.keySet()) {
                if (o.equals("200")) {
                    var maybeContent = responses.mappingNode(o).required().mappingNode("content");
                    if (maybeContent.isPresent()) {
                        var content = maybeContent.required();
                        for (var contentType : content.keySet()) {
                            var codegenContent = operation.addResponseType(contentType);
                            var schema = content.mappingNode(contentType).required().mappingNode("schema").required();
                            codegenContent.setType(getType(schema, null, null));
                        }
                    }
                }
            }
        }
        if (operationNode.containsKey("security")) {
            for (var securityNode : operationNode.sequenceNode("security").required().mappingNodes()) {
                for (var key : securityNode.keySet()) {
                    var security = operation.addSecurity(key);
                    securityNode.sequenceNode("key").map(SpecSequenceNode::stringList)
                            .ifPresent(security::setScopes);
                }
            }
        }
    }

    private void readSecuritySchemes(OpenapiSpec spec, SpecMappingNode node) {
        if (node.containsKey("components")) {
            var componentsNode = node.mappingNode("components").required();
            if (componentsNode.containsKey("securitySchemes")) {
                var securitySchemesNode = componentsNode.mappingNode("securitySchemes").required();
                for (var scheme : securitySchemesNode.keySet()) {
                    var schemeNode = securitySchemesNode.mappingNode(scheme).required();
                    var securityScheme = spec.addSecurityScheme(scheme);
                    securityScheme.setType(schemeNode.string("type").required());
                }
            }
        }
    }



    private static CodegenType getType(SpecMappingNode schema, CodegenPropertyMap model, CodegenProp prop) {
        var $ref = schema.string("$ref");

        if ($ref.isPresent()) {
            return new CodegenTypeRef($ref.required());
        }
        if (schema.containsKey("enum")) {
            var values = schema.sequenceNode("enum").required().stringList();
            if (values.size() == 1) {
                return new CodegenConstantType(values.get(0));
            }
            var result = new CodegenInlineEnumType();
            result.setType(schema.string("type").orElse("string"));
            result.getValues().addAll(values);
            result.setDeclaredModel(model);
            result.setDeclaredProperty(prop);
            schema.string("description").ifPresent(result::setDescription);
            return result;
        } else if (schema.containsKey("properties")) {
            var result = new CodegenInlineObjectType();
            readProperties(schema, result);
            return result;
        } else if (schema.containsKey("additionalProperties")) {
            var result = new CodegenRecordType();
            result.setAdditionalProperties(getType(schema.mappingNode("additionalProperties").required(), model, prop));
            return result;
        } else {
            var type = schema.string("type").required();
            if ("array".equals(type)) {
                var result = new CodegenArrayType();
                result.setUniqueItems(schema.getBoolean("uniqueItems").orElse(false));
                result.setItems(getType(schema.mappingNode("items").required(), model, prop));
                return result;
            } else {
                var result = new CodegenPrimitiveType();
                result.setType(type);
                result.setFormat(schema.string("format").orNull());
                return result;
            }
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

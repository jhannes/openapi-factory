package org.openapifactory.api;

import org.openapifactory.api.codegen.types.CodegenAnonymousObjectModel;
import org.openapifactory.api.codegen.types.CodegenArrayType;
import org.openapifactory.api.codegen.types.CodegenConstantType;
import org.openapifactory.api.codegen.CodegenContact;
import org.openapifactory.api.codegen.types.CodegenEmbeddedEnumType;
import org.openapifactory.api.codegen.CodegenOperation;
import org.openapifactory.api.codegen.CodegenParameter;
import org.openapifactory.api.codegen.types.CodegenPrimitiveType;
import org.openapifactory.api.codegen.CodegenProp;
import org.openapifactory.api.codegen.CodegenPropertyModel;
import org.openapifactory.api.codegen.types.CodegenRecordType;
import org.openapifactory.api.codegen.types.CodegenType;
import org.openapifactory.api.codegen.types.CodegenTypeRef;
import org.openapifactory.api.codegen.OpenapiSpec;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class OpenapiSpecParser {

    public OpenapiSpec createOpenApiSpec(Path apiDocument) throws IOException {
        return createSpec(SpecMappingNode.read(apiDocument));
    }

    protected OpenapiSpec createSpec(SpecMappingNode node) {
        var spec = new OpenapiSpec();
        readSpec(node, spec);
        return spec;
    }

    protected void readSpec(SpecMappingNode node, OpenapiSpec spec) {
        readInfo(node.mappingNode("info").required(), spec);
        readServers(node.sequenceNode("servers"), spec);
        readModels(node, spec);
        readPaths(node.mappingNode("paths").required(), spec);
        readSecuritySchemes(node, spec);
    }

    protected void readServers(Maybe<SpecSequenceNode> servers, OpenapiSpec spec) {
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

    private void readModels(SpecMappingNode node, OpenapiSpec spec) {
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
            enumModel.setType(node.string("type").orElse("string"));
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
                        var mappingValue = mapping.string(mappingKey).required();
                        if (CodegenTypeRef.REF_PATTERN.matcher(mappingValue).matches()) {
                            oneOf.addMapping(mappingKey, new CodegenTypeRef(oneOf.getSpec(), mappingValue));
                        } else {
                            oneOf.addMapping(mappingKey, new CodegenTypeRef(oneOf.getSpec(), "#/components/schemas/" + mappingValue));
                        }
                    }
                }
            }
        } else {
            throw new IllegalArgumentException("Unsupported model " + modelName + ": " + node);
        }
    }

    private static void readProperties(SpecMappingNode node, CodegenPropertyModel model) {
        var required = node.sequenceNode("required")
                .map(SpecSequenceNode::stringList)
                .orElse(List.of());
        var properties = node.mappingNode("properties").required();
        for (var name : properties.keySet()) {
            var prop = model.addProperty(name);
            if (required.contains(name)) {
                prop.setRequired(true);
            }
            var propNode = properties.mappingNode(name).required();
            prop.setDescription(propNode.string("description").orNull());
            prop.setExample(propNode.string("example").orNull());
            prop.setType(getType(prop.getSpec(), propNode, model, prop));
            prop.setReadOnly(propNode.getBoolean("readOnly").orElse(false));
            prop.setWriteOnly(propNode.getBoolean("writeOnly").orElse(false));
        }
    }

    protected void readPaths(SpecMappingNode paths, OpenapiSpec spec) {
        for (var pathExpression : paths.keySet()) {
            var pathNode = paths.mappingNode(pathExpression).required();
            var specialValues = Set.of("$ref", "summary", "description", "servers", "parameters");

            var commonParameters = new ArrayList<CodegenParameter>();
            var parameters = pathNode.sequenceNode("parameters");
            if (parameters.isPresent()) {
                for (var paramNode : parameters.required().mappingNodes()) {
                    var parameter = new CodegenParameter(spec, paramNode.string("name").required());
                    readParameter(paramNode, parameter);
                    commonParameters.add(parameter);
                }
            }

            for (var method : pathNode.keySet()) {
                if (specialValues.contains(method)) {
                    continue;
                }
                var operationNode = pathNode.mappingNode(method).required();
                var tags = operationNode.sequenceNode("tags")
                        .map(SpecSequenceNode::stringList)
                        .orElse(List.of("default"));

                var operation = spec.createOperation(method, pathExpression);
                operation.getParameters().addAll(commonParameters);
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
                readParameter(parameter, codegenParameter);
            }
        }

        if (operationNode.containsKey("requestBody")) {
            var requestBody = operationNode.mappingNode("requestBody").required();
            var content = requestBody.mappingNode("content").required();
            for (var contentType : content.keySet()) {
                var codegenContent = operation.addRequestBody(contentType);
                codegenContent.setRequired(requestBody.getBoolean("required").orElse(false));
                var schema = (content.mappingNode(contentType).required()).mappingNode("schema").required();
                var requestType = getType(operation.getSpec(), schema, null, null);
                if (!codegenContent.isFormContent() && requestType instanceof CodegenAnonymousObjectModel object) {
                    codegenContent.setType(operation.addRequestModel(object));
                } else {
                    codegenContent.setType(requestType);
                }
            }
        }
        if (operationNode.containsKey("responses")) {
            var responses = operationNode.mappingNode("responses").required();
            for (var o : responses.keySet()) {
                var maybeContent = responses.mappingNode(o).required().mappingNode("content");
                var response = operation.addResponse(o.equals("default") ? 200 : Integer.parseInt(o));
                if (maybeContent.isPresent()) {
                    var content = maybeContent.required();
                    for (var contentType : content.keySet()) {
                        var codegenContent = response.addResponseType(contentType);
                        var schema = content.mappingNode(contentType).required().mappingNode("schema").required();
                        var responseType = getType(operation.getSpec(), schema, null, null);
                        if (responseType instanceof CodegenAnonymousObjectModel object) {
                            codegenContent.setType(operation.addResponseModel(object, Integer.parseInt(o)));
                        } else {
                            codegenContent.setType(responseType);
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

    private static void readParameter(SpecMappingNode parameterNode, CodegenParameter parameter) {
        parameter.setRequired(parameterNode.getBoolean("required").orElse(false));
        parameter.setIn(parameterNode.getEnum("in", CodegenParameter.ParameterLocation.class).required());
        parameter.setExplode(parameterNode.getBoolean("explode").orElse(true));
        parameter.setStyle(parameterNode.getEnum("style", CodegenParameter.Style.class).orNull());
        parameter.setType(getType(parameter.getSpec(), parameterNode.mappingNode("schema").required(), null, parameter));
    }

    private void readSecuritySchemes(SpecMappingNode node, OpenapiSpec spec) {
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


    private static CodegenType getType(OpenapiSpec spec, SpecMappingNode schema, CodegenPropertyModel model, CodegenProp prop) {
        if (schema.containsKey("$ref")) {
            return new CodegenTypeRef(spec, schema.string("$ref").required());
        }
        if (schema.containsKey("enum")) {
            var values = schema.sequenceNode("enum").required().stringList();
            if (values.size() == 1) {
                return new CodegenConstantType(values.get(0));
            }
            var result = new CodegenEmbeddedEnumType();
            result.setType(schema.string("type").orElse("string"));
            result.getValues().addAll(values);
            result.setDeclaredModel(model);
            result.setDeclaredProperty(prop);
            schema.string("description").ifPresent(result::setDescription);
            return result;
        } else if (schema.containsKey("properties")) {
            var result = new CodegenAnonymousObjectModel(spec);
            readProperties(schema, result);
            return result;
        } else if (schema.containsKey("additionalProperties")) {
            var result = new CodegenRecordType();
            result.setAdditionalProperties(getType(spec, schema.mappingNode("additionalProperties").required(), model, prop));
            return result;
        } else {
            var type = schema.string("type").required();
            if ("array".equals(type)) {
                var result = new CodegenArrayType();
                result.setUniqueItems(schema.getBoolean("uniqueItems").orElse(false));
                result.setItems(getType(spec, schema.mappingNode("items").required(), model, prop));
                return result;
            } else {
                var result = new CodegenPrimitiveType();
                result.setType(type);
                result.setFormat(schema.string("format").orNull());
                return result;
            }
        }
    }

    private static void readInfo(SpecMappingNode infoNode, OpenapiSpec spec) {
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

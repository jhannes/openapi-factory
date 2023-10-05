package org.openapifactory.typescript;

import org.openapifactory.api.StringUtil;
import org.openapifactory.api.codegen.CodegenArrayType;
import org.openapifactory.api.codegen.CodegenConstantType;
import org.openapifactory.api.codegen.CodegenInlineObjectType;
import org.openapifactory.api.codegen.CodegenModel;
import org.openapifactory.api.codegen.CodegenParameter;
import org.openapifactory.api.codegen.CodegenPrimitiveType;
import org.openapifactory.api.codegen.CodegenProp;
import org.openapifactory.api.codegen.CodegenRecordType;
import org.openapifactory.api.codegen.CodegenType;
import org.openapifactory.api.codegen.CodegenTypeRef;
import org.openapifactory.api.codegen.OpenapiSpec;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.openapifactory.api.StringUtil.join;

public class TypescriptFragments {
    public static String propertyDefinition(CodegenProp p) {
        return getName(p) + (p.isRequired() && !p.getType().hasOnlyOptionalProperties() ? "" : "?") + ": " + getTypeName(p.getType());
    }

    public static String getName(CodegenProp p) {
        if (p.getName() == null) {
            if (p.getType() instanceof CodegenTypeRef refType) {
                return StringUtil.toLowerCamelCase(refType.getClassName() + "Dto");
            }
        }
        return p.getName();
    }

    public static String getTypeName(CodegenType type) {
        if (type instanceof CodegenTypeRef refType) {
            return refType.getClassName() + "Dto";
        } else if (type instanceof CodegenInlineObjectType objectType) {
            return "{ " + join("; ", objectType.getProperties().values(), TypescriptFragments::propertyDefinition) + "; }";
        } else if (type instanceof CodegenModel model) {
            return model.getName();
        } else if (type instanceof CodegenArrayType arrayType) {
            return (arrayType.isUniqueItems() ? "Set" : "Array") + "<" + getTypeName(arrayType.getItems()) + ">";
        } else if (type instanceof CodegenConstantType constant) {
            return "\"" + constant.getValue() + "\"";
        } else if (type instanceof CodegenPrimitiveType primitive) {
            if (primitive.getType().equals("string") && primitive.getFormat() != null) {
                return Map.of("date-time", "Date", "date", "Date", "binary", "Blob")
                        .getOrDefault(primitive.getFormat(), primitive.getType());
            } else {
                return Map.of("integer", "number", "float", "number", "object", "unknown")
                        .getOrDefault(primitive.getType(), primitive.getType());
            }
        } else if (type instanceof CodegenRecordType objectType) {
            return "{ [key: string]: " + getTypeName(objectType.getAdditionalProperties()) + "; }";
        } else {
            throw new IllegalArgumentException("Not supported " + type);
        }
    }

    public static String propertiesDefinition(List<CodegenParameter> parameters) {
        return parameters
                .stream()
                .map(TypescriptFragments::propertyDefinition)
                .collect(Collectors.joining(", "));
    }

    public static String documentationSection(OpenapiSpec spec) {
        var contact = spec.getContact().map(c -> " Contact: " + c.getEmail()).orElse("");
        return """
                /**
                 * %s
                 * %s
                 *
                 * The version of the OpenAPI document: %s
                 *%s
                 *
                 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
                 * https://openapi-generator.tech
                 * Do not edit the class manually.
                 */
                """.formatted(spec.getTitle(), spec.getDescription(), spec.getVersion(), contact);
    }
}

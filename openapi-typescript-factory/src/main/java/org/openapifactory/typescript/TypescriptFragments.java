package org.openapifactory.typescript;

import org.openapifactory.api.codegen.CodegenApi;
import org.openapifactory.api.codegen.CodegenProp;
import org.openapifactory.api.codegen.CodegenProperty;
import org.openapifactory.api.codegen.OpenapiSpec;
import org.openapifactory.api.codegen.types.CodegenAnonymousObjectModel;
import org.openapifactory.api.codegen.types.CodegenArraySchema;
import org.openapifactory.api.codegen.types.CodegenConstantSchema;
import org.openapifactory.api.codegen.types.CodegenEmbeddedEnumSchema;
import org.openapifactory.api.codegen.types.CodegenModel;
import org.openapifactory.api.codegen.types.CodegenPrimitiveSchema;
import org.openapifactory.api.codegen.types.CodegenRecordSchema;
import org.openapifactory.api.codegen.types.CodegenSchema;
import org.openapifactory.api.codegen.types.CodegenSchemaRef;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.openapifactory.api.StringUtil.join;
import static org.openapifactory.api.StringUtil.toLowerCamelCase;
import static org.openapifactory.api.StringUtil.toUpperCamelCase;

public class TypescriptFragments {
    public static String propertyDefinition(CodegenProp p) {
        return getPropName(p) + (p.isRequired() && p.getSchema().hasNoRequiredProperties() ? "" : "?") +
               ": " + getTypeName(p.getSchema()) + (p.isNullable() ? " | null" : "");
    }

    public static String variableName(CodegenSchema type) {
        if (type instanceof CodegenAnonymousObjectModel) {
            return "dto";
        }
        if (type instanceof CodegenArraySchema arrayType) {
            return variableName(arrayType.getItems());
        }
        return toLowerCamelCase(getTypeName(type));
    }

    public static String getPropName(CodegenProp p) {
        if (p.getName() == null) {
            return variableName(p.getSchema());
        }
        return p.getName();
    }

    public static String getTypeName(CodegenSchema type) {
        if (type instanceof CodegenSchemaRef refType) {
            return getTypeName(refType.getReferencedType());
        } else if (type instanceof CodegenArraySchema arrayType) {
            var itemTypeName = getTypeName(arrayType.getItems());
            if (arrayType.getMaxItems() != null && arrayType.getMaxItems() < 5) {
                return "[" + IntStream.range(0, arrayType.getMaxItems())
                        .mapToObj(i -> itemTypeName + (i >= arrayType.getMinItems() ? "?" : ""))
                        .collect(Collectors.joining(", ")) +
                       "]";
            }
            return getCollectionType(arrayType) + "<" + itemTypeName + ">";
        } else if (type instanceof CodegenModel model) {
            return model.getName() + "Dto";
        } else if (type instanceof CodegenAnonymousObjectModel objectType) {
            return "{ " + join("; ", objectType.getAllProperties(), TypescriptFragments::propertyDefinition) + " }";
        } else if (type instanceof CodegenEmbeddedEnumSchema enumModel) {
            if (enumModel.getDeclaredProperty() instanceof CodegenProperty prop) {
                return getTypeName(prop.getModel()) + toUpperCamelCase(prop.getName()) + "Enum";
            }
            return join(" | ", enumModel.getValues(), s -> "\"" + s + "\"");
        } else if (type instanceof CodegenRecordSchema objectType) {
            return "{ [key: string]: " + getTypeName(objectType.getAdditionalProperties()) + "; }";
        } else if (type instanceof CodegenConstantSchema constant) {
            return "\"" + constant.getValue() + "\"";
        } else if (type instanceof CodegenPrimitiveSchema primitive) {
            if (primitive.getType().equals("string") && primitive.getFormat() != null) {
                return Map.of("date-time", "Date", "date", "Date", "binary", "Blob")
                        .getOrDefault(primitive.getFormat(), primitive.getType());
            } else {
                return Map.of("integer", "number", "float", "number", "object", "unknown")
                        .getOrDefault(primitive.getType(), primitive.getType());
            }
        } else {
            throw new IllegalArgumentException("Not supported " + type);
        }
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

    public static String getRequestTypeName(CodegenSchema type) {
        if (!type.hasReadOnlyProperties()) {
            return getTypeName(type);
        } else if (type instanceof CodegenArraySchema arrayType) {
            return getCollectionType(arrayType) + "<" + getRequestTypeName(arrayType.getItems()) + ">";
        }
        return getTypeName(type) + "Request";
    }

    public static String getResponseTypeName(CodegenSchema type) {
        if (!type.hasWriteOnlyProperties()) {
            return getTypeName(type);
        } else if (type instanceof CodegenArraySchema arrayType) {
            return getCollectionType(arrayType) + "<" + getResponseTypeName(arrayType.getItems()) + ">";
        }
        return getTypeName(type) + "Response";
    }

    private static String getCollectionType(CodegenArraySchema arrayType) {
        return arrayType.isUniqueItems() ? "Set" : "Array";
    }

    public static String getApiName(CodegenApi api) {
        return toUpperCamelCase(api.getTag()) + "Api";
    }

    public static String docString(String description) {
        if (description == null) return "";
        return "/**\n * " + description + "\n */\n";
    }
}

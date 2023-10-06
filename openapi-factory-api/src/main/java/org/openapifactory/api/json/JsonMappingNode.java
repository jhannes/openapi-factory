package org.openapifactory.api.json;

import jakarta.json.Json;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import org.openapifactory.api.Maybe;
import org.openapifactory.api.SpecMappingNode;
import org.openapifactory.api.SpecSequenceNode;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class JsonMappingNode implements SpecMappingNode {

    private final List<String> path;
    private final JsonObject node;

    public JsonMappingNode(List<String> path, JsonObject node) {
        this.path = path;
        this.node = node;
    }

    public static SpecMappingNode read(Reader reader) {
        return new JsonMappingNode(List.of(), Json.createReader(reader).readObject());
    }

    @Override
    public Maybe<SpecMappingNode> mappingNode(String key) {
        return get(key).filter(
                n -> n instanceof JsonObject,
                n -> "Expected JsonObject at " + path + " " + key + ": " + n
        ).map(n -> new JsonMappingNode(appendToPath(key), (JsonObject) n));
    }

    private Maybe<JsonValue> get(String key) {
        return containsKey(key) ? Maybe.present(node.get(key)) : missingKey(key);
    }

    @Override
    public Maybe<SpecSequenceNode> sequenceNode(String key) {
        if (!containsKey(key)) {
            return missingKey(key);
        }
        return Maybe.present(new JsonSequenceNode(appendToPath(key), node.getJsonArray(key)));
    }

    @Override
    public Maybe<String> string(String key) {
        if (!containsKey(key)) {
            return missingKey(key);
        }
        return Maybe.present(getString(key));
    }

    private String getString(String key) {
        var v = node.get(key);
        if (v instanceof JsonString string) {
            return string.getString();
        } else if (v instanceof JsonNumber number) {
            return number.toString();
        } else if (v == JsonValue.TRUE || v == JsonValue.FALSE) {
            return v.toString();
        }
        throw new RuntimeException("Expected key [" + key + "] at " + path + " value " + v + " to be String");
    }

    @Override
    public <T extends Enum<T>> Maybe<T> getEnum(String key, Class<T> enumType) {
        return string(key).map(s -> asEnum(enumType, s, key));
    }

    @Override
    public Set<String> keySet() {
        return node.keySet();
    }

    private <T extends Enum<T>> T asEnum(Class<T> enumType, String s, String key) {
        try {
            return Enum.valueOf(enumType, s);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Expected key [" + key + "] at " + path + " value " + s + " to be " + Arrays.toString(enumType.getEnumConstants()));
        }
    }

    private <T> Maybe<T> missingKey(String key) {
        return Maybe.missing("required " + key + " in " + path);
    }

    private List<String> appendToPath(String key) {
        var result = new ArrayList<>(path);
        result.add(key);
        return result;
    }
}

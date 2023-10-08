package org.openapifactory.api.parser.json;

import jakarta.json.Json;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import org.openapifactory.api.parser.Maybe;
import org.openapifactory.api.parser.SpecMappingNode;
import org.openapifactory.api.parser.SpecSequenceNode;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JsonMappingNode implements SpecMappingNode {

    private final List<String> path;
    private final JsonObject node;
    private final String relativeFilename;
    private final Set<String> unusedKeys = new HashSet<>();

    public JsonMappingNode(List<String> path, JsonObject node, String relativeFilename) {
        this.path = path;
        this.node = node;
        this.relativeFilename = relativeFilename;
        unusedKeys.addAll(node.keySet());
    }

    public static SpecMappingNode read(Reader reader, String relativeFile) {
        return new JsonMappingNode(List.of(), Json.createReader(reader).readObject(), relativeFile);
    }

    @Override
    public String getRelativeFilename() {
        return relativeFilename;
    }

    @Override
    public Maybe<SpecMappingNode> mappingNode(String key) {
        return get(key).filter(
                n -> n instanceof JsonObject,
                n -> "Expected JsonObject at " + path + " " + key + ": " + n
        ).map(n -> new JsonMappingNode(appendToPath(key), (JsonObject) n, relativeFilename));
    }

    private Maybe<JsonValue> get(String key) {
        unusedKeys.remove(key);
        return containsKey(key) ? Maybe.present(node.get(key)) : missingKey(key);
    }

    @Override
    public Maybe<SpecSequenceNode> sequenceNode(String key) {
        if (!containsKey(key)) {
            return missingKey(key);
        }
        unusedKeys.remove(key);
        return Maybe.present(new JsonSequenceNode(appendToPath(key), node.getJsonArray(key), relativeFilename));
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
        unusedKeys.remove(key);
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

    @Override
    public boolean isObject(String key) {
        return containsKey(key) && node.get(key) instanceof JsonObject;
    }

    @Override
    public void checkUnused() {
        if (!unusedKeys.isEmpty()) {
            throw new RuntimeException("Unused keys " + unusedKeys + " in " + getPath());
        }
    }

    private <T extends Enum<T>> T asEnum(Class<T> enumType, String s, String key) {
        try {
            return Enum.valueOf(enumType, s);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(
                    "expected enum value [" + s + "] to be in " + Arrays.toString(enumType.getEnumConstants()) + " at " + getPath() + "/" + key
            );
        }
    }

    private <T> Maybe<T> missingKey(String key) {
        return Maybe.missing("required " + key + " in " + getPath());
    }

    private List<String> appendToPath(String key) {
        var result = new ArrayList<>(path);
        result.add(key);
        return result;
    }

    private String getPath() {
        return relativeFilename + "#/" + String.join("/", path);
    }
}

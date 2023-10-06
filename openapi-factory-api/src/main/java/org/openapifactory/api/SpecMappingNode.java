package org.openapifactory.api;

import org.openapifactory.api.json.JsonMappingNode;
import org.openapifactory.api.yaml.YamlMappingNode;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

public interface SpecMappingNode {
    static SpecMappingNode read(Path apiDocument) throws IOException {
        var filename = apiDocument.getFileName().toString();
        if (filename.endsWith(".yaml")) {
            return YamlMappingNode.read(apiDocument);
        } else if (filename.endsWith(".json")) {
            return JsonMappingNode.read(apiDocument);
        } else {
            throw new RuntimeException("Unsupported file format " + apiDocument);
        }
    }

    Maybe<SpecMappingNode> mappingNode(String key);

    Maybe<SpecSequenceNode> sequenceNode(String key);

    Maybe<String> string(String key);

    default Maybe<Boolean> getBoolean(String key) {
        return string(key).map("true"::equals);
    }

    <T extends Enum<T>> Maybe<T> getEnum(String key, Class<T> enumType);

    Set<String> keySet();

    default boolean containsKey(String key) {
        return keySet().contains(key);
    }
}

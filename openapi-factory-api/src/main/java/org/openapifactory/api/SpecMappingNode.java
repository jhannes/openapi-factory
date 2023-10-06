package org.openapifactory.api;

import org.openapifactory.api.json.JsonMappingNode;
import org.openapifactory.api.yaml.YamlMappingNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Path;
import java.util.Set;

public interface SpecMappingNode {
    static SpecMappingNode read(Path apiDocument) throws IOException {
        return read(apiDocument.toUri().toURL());
    }

    static SpecMappingNode read(URL url) throws IOException {
        var filename = url.getFile();
        var connection = url.openConnection();
        try (var reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            if (filename.endsWith(".yaml")) {
                return YamlMappingNode.read(reader);
            } else if (filename.endsWith(".json")) {
                return JsonMappingNode.read(reader);
            } else if (filename.endsWith(".link")) {
                return read(new URL(reader.readLine().trim()));
            } else {
                throw new RuntimeException("Unsupported file format " + url);
            }
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

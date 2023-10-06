package org.openapifactory.api.yaml;

import org.openapifactory.api.Maybe;
import org.openapifactory.api.SpecMappingNode;
import org.openapifactory.api.SpecSequenceNode;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.composer.Composer;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.parser.ParserImpl;
import org.yaml.snakeyaml.reader.StreamReader;
import org.yaml.snakeyaml.resolver.Resolver;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class YamlMappingNode implements SpecMappingNode {
    private final List<String> path;
    private final MappingNode mappingNode;
    private final Map<String, Node> nodeMap = new LinkedHashMap<>();

    public YamlMappingNode(List<String> path, Node mappingNode) {
        this.path = path;
        if (!(mappingNode instanceof MappingNode)) {
            throw new RuntimeException(
                    "Expected node to be mapping at " + path + " " + mappingNode.getStartMark()
            );
        }
        this.mappingNode = (MappingNode) mappingNode;

        for (var nodeTuple : this.mappingNode.getValue()) {
            var keyNode = (ScalarNode) nodeTuple.getKeyNode();
            nodeMap.put(keyNode.getValue(), nodeTuple.getValueNode());
        }

    }

    public static SpecMappingNode read(Reader reader) {
        var composer =
                new Composer(new ParserImpl(new StreamReader(reader), new LoaderOptions()), new Resolver(), new LoaderOptions());
        return new YamlMappingNode(List.of(), composer.getSingleNode());
    }

    @Override
    public Maybe<SpecMappingNode> mappingNode(String key) {
        if (!containsKey(key)) {
            return missingKey(key);
        }
        return Maybe.present(new YamlMappingNode(append(path, key), nodeMap.get(key)));
    }

    @Override
    public Maybe<SpecSequenceNode> sequenceNode(String key) {
        if (!containsKey(key)) {
            return missingKey(key);
        }
        return Maybe.present(new YamlSequenceNode(append(path, key), nodeMap.get(key)));
    }

    @Override
    public Maybe<String> string(String key) {
        if (!containsKey(key)) {
            return missingKey(key);
        }
        var node = nodeMap.get(key);
        if (node instanceof ScalarNode scalar) {
            return Maybe.present(scalar.getValue());
        } else {
            throw new RuntimeException(
                    "Expected key " + key + " to be scalar at " + path + " " + node
            );
        }
    }

    @Override
    public <T extends Enum<T>> Maybe<T> getEnum(String key, Class<T> enumType) {
        return string(key).map(s -> asEnum(enumType, s, key));
    }

    @Override
    public Set<String> keySet() {
        return nodeMap.keySet();
    }

    private <T extends Enum<T>> T asEnum(Class<T> enumType, String s, String key) {
        try {
            return Enum.valueOf(enumType, s);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Expected key " + key + " at " + path + " value " + s + " to be " + Arrays.toString(enumType.getEnumConstants()));
        }
    }

    private <T> Maybe<T> missingKey(String key) {
        return Maybe.missing("required " + key + " in " + path + " " + mappingNode.getStartMark() + " (keys: " + nodeMap.keySet() + ")");
    }

    private static List<String> append(List<String> path, String key) {
        var result = new ArrayList<>(path);
        result.add(key);
        return result;
    }
}

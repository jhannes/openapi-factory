package org.openapifactory.api.parser.yaml;

import org.openapifactory.api.parser.Maybe;
import org.openapifactory.api.parser.SpecMappingNode;
import org.openapifactory.api.parser.SpecSequenceNode;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.composer.Composer;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.parser.ParserImpl;
import org.yaml.snakeyaml.reader.StreamReader;
import org.yaml.snakeyaml.resolver.Resolver;

import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class YamlMappingNode implements SpecMappingNode {
    private final List<String> path;
    private final MappingNode mappingNode;
    private final Map<String, Node> nodeMap = new LinkedHashMap<>();
    private final Set<String> unusedKeys = new HashSet<>();
    private final String relativeFilename;
    private final URL url;

    public YamlMappingNode(List<String> path, Node mappingNode, String relativeFilename, URL url) {
        this.path = path;
        this.relativeFilename = relativeFilename;
        this.url = url;
        if (!(mappingNode instanceof MappingNode)) {
            throw new RuntimeException(
                    "Expected node to be mapping at " + path + " " + mappingNode.getStartMark()
            );
        }
        this.mappingNode = (MappingNode) mappingNode;

        for (var nodeTuple : this.mappingNode.getValue()) {
            var keyNode = (ScalarNode) nodeTuple.getKeyNode();
            nodeMap.put(keyNode.getValue(), nodeTuple.getValueNode());
            unusedKeys.addAll(nodeMap.keySet());
        }
    }

    public static SpecMappingNode read(Reader reader, String relativeFilename, URL url) {
        var composer =
                new Composer(new ParserImpl(new StreamReader(reader), new LoaderOptions()), new Resolver(), new LoaderOptions());
        return new YamlMappingNode(List.of(), composer.getSingleNode(), relativeFilename, url);
    }

    @Override
    public String getRelativeFilename() {
        return relativeFilename;
    }

    @Override
    public Maybe<SpecMappingNode> mappingNode(String key) {
        if (!containsKey(key)) {
            return missingKey(key);
        }
        return Maybe.present(new YamlMappingNode(append(path, key), getNode(key), relativeFilename, url));
    }

    @Override
    public Maybe<SpecSequenceNode> sequenceNode(String key) {
        if (!containsKey(key)) {
            return missingKey(key);
        }
        return Maybe.present(new YamlSequenceNode(append(path, key), getNode(key), relativeFilename, url));
    }

    @Override
    public Maybe<String> string(String key) {
        if (!containsKey(key)) {
            return missingKey(key);
        }
        return asString(getNode(key));
    }

    private Maybe<String> asString(Node node) {
        if (node instanceof ScalarNode scalar) {
            return Maybe.present(scalar.getValue());
        } else {
            throw new RuntimeException(
                    "Expected value " + node + " to be scalar at " + getPath() + " " + getFilePath(node)
            );
        }
    }

    @Override
    public <T extends Enum<T>> Maybe<T> getEnum(String key, Class<T> enumType) {
        if (!containsKey(key)) {
            return missingKey(key);
        }
        var node = getNode(key);
        return Maybe.present(asEnum(enumType, asString(node).required(), node));
    }

    private <T extends Enum<T>> T asEnum(Class<T> enumType, String s, Node node) {
        try {
            return Enum.valueOf(enumType, s);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(
                    "expected enum value [" + s + "] to be in " + Arrays.toString(enumType.getEnumConstants()) + " at " + getPath() + " " + getFilePath(node)
            );
        }
    }

    @Override
    public Set<String> keySet() {
        return nodeMap.keySet();
    }

    @Override
    public boolean isObject(String key) {
        return containsKey(key) && getNode(key) instanceof MappingNode;
    }

    @Override
    public void checkUnused() {
        if (!unusedKeys.isEmpty()) {
            throw new RuntimeException("Unused keys " + unusedKeys + " in " + getPath() + " " + getFilePath(mappingNode));
        }
    }

    private <T> Maybe<T> missingKey(String key) {
        return Maybe.missing("missing required key [" + key + "] (keys: " + keySet() + ") at " +  getPath() + " " + getFilePath(mappingNode));
    }

    private Node getNode(String key) {
        unusedKeys.remove(key);
        return nodeMap.get(key);
    }

    private static List<String> append(List<String> path, String key) {
        var result = new ArrayList<>(path);
        result.add(key);
        return result;
    }

    private String getFilePath(Node node) {
        return url + ":" + (node.getStartMark().getLine() + 1);
    }

    private String getPath() {
        return relativeFilename + "#/" + String.join("/", path);
    }

    @Override
    public String toString() {
        return "YamlMappingNode{path=" + path + ", nodeMap=" + nodeMap.keySet() + '}';
    }
}

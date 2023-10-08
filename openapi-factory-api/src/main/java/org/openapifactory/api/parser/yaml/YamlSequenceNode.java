package org.openapifactory.api.parser.yaml;

import org.openapifactory.api.parser.SpecMappingNode;
import org.openapifactory.api.parser.SpecSequenceNode;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class YamlSequenceNode implements SpecSequenceNode {
    private final List<String> path;
    private final SequenceNode node;
    private final String relativeFilename;
    private final URL url;

    public YamlSequenceNode(List<String> path, Node node, String relativeFilename, URL url) {
        this.path = path;
        this.relativeFilename = relativeFilename;
        this.url = url;
        if (node instanceof MappingNode) {
            throw new RuntimeException(
                    "Expected node to be sequence at " + path + " " + node.getStartMark()
            );
        }
        this.node = (SequenceNode) node;
    }

    @Override
    public Iterable<SpecMappingNode> mappingNodes() throws IllegalArgumentException {
        var result = new ArrayList<SpecMappingNode>();
        var value = node.getValue();
        for (int i = 0; i < value.size(); i++) {
            result.add(new YamlMappingNode(append(path, i), value.get(i), relativeFilename, url));
        }
        return result;
    }

    @Override
    public List<String> stringList() throws IllegalArgumentException {
        var result = new ArrayList<String>();
        var value = node.getValue();
        for (int i = 0; i < value.size(); i++) {
            var node = value.get(i);
            if (node instanceof ScalarNode scalar) {
                result.add(scalar.getValue());
            } else {
                throw new IllegalArgumentException(
                        "Expected index " + i + " of " + path + " at " + node.getStartMark() + " to be scalar. Was " + node
                );
            }
        }
        return result;
    }

    private static List<String> append(List<String> path, int index) {
        var result = new ArrayList<>(path);
        result.add(String.valueOf(index));
        return result;
    }
}

package org.openapifactory.api.parser.json;

import jakarta.json.JsonArray;
import jakarta.json.JsonNumber;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import org.openapifactory.api.parser.SpecMappingNode;
import org.openapifactory.api.parser.SpecSequenceNode;

import java.util.ArrayList;
import java.util.IllegalFormatException;
import java.util.List;

public class JsonSequenceNode implements SpecSequenceNode {
    private final List<String> path;
    private final JsonArray node;
    private final String relativeFile;

    public JsonSequenceNode(List<String> path, JsonArray node, String relativeFile) {
        this.path = path;
        this.node = node;
        this.relativeFile = relativeFile;
    }

    @Override
    public Iterable<SpecMappingNode> mappingNodes() throws IllegalFormatException {
        var result = new ArrayList<SpecMappingNode>();
        for (int i = 0; i < node.size(); i++) {
            result.add(new JsonMappingNode(appendToPath(i), node.getJsonObject(i), relativeFile));
        }
        return result;
    }

    @Override
    public List<String> stringList() throws IllegalFormatException {
        return node.stream().map(this::asString).toList();
    }

    private String asString(JsonValue v) {
        if (v instanceof JsonString string) {
            return string.getString();
        } else if (v instanceof JsonNumber number) {
            return number.toString();
        } else if (v == JsonValue.TRUE || v == JsonValue.FALSE) {
            return v.toString();
        }
        throw new RuntimeException("Expected at " + path + " value " + v + " to be String");
    }


    private List<String> appendToPath(int index) {
        var result = new ArrayList<>(path);
        result.add(String.valueOf(index));
        return result;
    }
}

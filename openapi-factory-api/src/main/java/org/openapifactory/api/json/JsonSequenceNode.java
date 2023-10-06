package org.openapifactory.api.json;

import jakarta.json.JsonArray;
import jakarta.json.JsonNumber;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import org.openapifactory.api.SpecMappingNode;
import org.openapifactory.api.SpecSequenceNode;

import java.util.ArrayList;
import java.util.IllegalFormatException;
import java.util.List;

public class JsonSequenceNode implements SpecSequenceNode {
    private final List<String> path;
    private final JsonArray node;

    public JsonSequenceNode(List<String> path, JsonArray node) {
        this.path = path;
        this.node = node;
    }

    @Override
    public Iterable<SpecMappingNode> mappingNodes() throws IllegalFormatException {
        var result = new ArrayList<SpecMappingNode>();
        for (int i = 0; i < node.size(); i++) {
            result.add(new JsonMappingNode(appendToPath(i), node.getJsonObject(i)));
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

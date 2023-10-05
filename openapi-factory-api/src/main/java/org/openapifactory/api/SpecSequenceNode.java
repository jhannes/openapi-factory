package org.openapifactory.api;

import java.util.IllegalFormatException;
import java.util.List;

public interface SpecSequenceNode {
    Iterable<SpecMappingNode> mappingNodes() throws IllegalFormatException;

    List<String> stringList() throws IllegalFormatException;
}

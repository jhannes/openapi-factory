package org.openapifactory.api;

import java.util.Collection;
import java.util.IllegalFormatException;

public interface SpecSequenceNode {
    Iterable<SpecMappingNode> mappingNodes() throws IllegalFormatException;

    Collection<String> stringList() throws IllegalFormatException;
}

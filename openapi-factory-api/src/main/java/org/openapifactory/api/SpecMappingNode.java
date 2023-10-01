package org.openapifactory.api;

public interface SpecMappingNode {
    Maybe<SpecMappingNode> mappingNode(String key);

    Maybe<SpecSequenceNode> sequenceNode(String key);

    Maybe<String> string(String key);

    Maybe<Boolean> getBoolean(String key);

    <T extends Enum<T>> Maybe<T> getEnum(String key, Class<T> enumType);

    Iterable<String> keySet();

    boolean containsKey(String key);
}

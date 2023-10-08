package org.openapifactory.typescript;

import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.openapifactory.api.OpenapiFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static org.openapifactory.test.OpenapiSnapshotNode.singleSnapshotTest;

public class FocusedTest {

    @TestFactory
    Stream<DynamicNode> typescriptApi() {
        var spec = Paths.get("../snapshotTests/conversations.yaml");
        return Stream.of(singleSnapshotTest(spec, createFactory(), Path.of("snapshotTests")));
    }

    private OpenapiFactory createFactory() {
        return new OpenapiTypescriptFactory();
    }

}

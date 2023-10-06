package org.openapifactory.typescript;

import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.openapifactory.api.OpenapiFactory;
import org.openapifactory.test.OpenapiSnapshotNode;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class FocusedTest {

    @TestFactory
    Stream<DynamicNode> typescriptApi() {
        Path spec = Paths.get("../snapshotTests/fakerestapi.link");
        return Stream.of(
                OpenapiSnapshotNode.singleSnapshotTest(spec, createFactory(), Path.of("snapshotTests"))
        );
    }

    private OpenapiFactory createFactory() {
        return new OpenapiTypescriptFactory();
    }

}

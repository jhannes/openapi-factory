package org.openapifactory.typescript;

import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.openapifactory.api.OpenapiFactory;
import org.openapifactory.test.OpenapiSnapshotNode;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class SnapshotTests {

    @TestFactory
    Stream<DynamicNode> typescriptApi() throws IOException {
        return Stream.of(
                snapshotTests(Paths.get("../snapshotTests"), Path.of("snapshotTests")),
                snapshotTests(Paths.get("../localSnapshotTests"), Path.of("localSnapshotTests"))
        );
    }

    private DynamicNode snapshotTests(Path specRoot, Path snapshotRoot) throws IOException {
        return OpenapiSnapshotNode.create(specRoot, createFactory(), snapshotRoot);
    }

    private OpenapiFactory createFactory() {
        return new OpenapiTypescriptFactory();
    }

}

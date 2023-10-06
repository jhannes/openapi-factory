package org.openapifactory.test;

import org.junit.jupiter.api.DynamicNode;
import org.openapifactory.api.OpenapiFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

public class OpenapiSnapshotNode {
    private final Path spec;
    private final OpenapiFactory factory;
    private final Path rootDirectory;

    public OpenapiSnapshotNode(Path spec, OpenapiFactory factory, Path parent) {
        this.spec = spec;
        this.factory = factory;
        rootDirectory = parent;
    }

    public static DynamicNode create(Path snapshotRoot, OpenapiFactory factory, Path rootDir) throws IOException {
        if (!Files.isDirectory(snapshotRoot)) {
            return dynamicTest("No snapshots for " + snapshotRoot, () -> {
            });
        }
        List<Path> list;
        try (var files = Files.list(snapshotRoot)) {
            list = files.toList();
        }
        return dynamicContainer(
                "Snapshots of " + snapshotRoot,
                list.stream().filter(Files::isRegularFile).map(spec -> singleSnapshotTest(spec, factory, rootDir))
        );
    }

    public static DynamicNode singleSnapshotTest(Path spec, OpenapiFactory factory, Path rootDir) {
        return new OpenapiSnapshotNode(spec, factory, rootDir).createTests();
    }

    private DynamicNode createTests() {
        try {
            cleanDirectory(getOutputDir());
            factory.generate(spec, getOutputDir());
        } catch (Exception e) {
            if (e.getCause() != null) {
                return dynamicTest("Generator for " + spec, () -> {
                    throw e.getCause();
                });
            }
            return dynamicTest("Generator for " + spec, () -> {
                throw e;
            });
        }

        return DiffNode.compareDirectories(getOutputDir(), getSnapshotDir(), "Snapshots for " + spec);
    }

    private Path getSnapshotDir() {
        return rootDirectory.resolve("snapshots").resolve(getModelName());
    }

    private Path getOutputDir() {
        return rootDirectory.resolve("output").resolve(getModelName());
    }

    private String getModelName() {
        var filename = spec.getFileName().toString();
        int lastDot = filename.lastIndexOf('.');
        return lastDot < 0 ? filename : filename.substring(0, lastDot);
    }

    private static void cleanDirectory(Path directory) throws IOException {
        if (Files.isDirectory(directory)) {
            try (Stream<Path> walk = Files.walk(directory)) {
                walk.sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        }
    }
}

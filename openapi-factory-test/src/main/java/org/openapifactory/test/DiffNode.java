package org.openapifactory.test;

import difflib.DeleteDelta;
import difflib.Delta;
import difflib.DiffUtils;
import difflib.InsertDelta;
import org.junit.jupiter.api.DynamicNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

public class DiffNode {
    private final Path outputDir;
    private final Path snapshotDir;

    public DiffNode(Path outputDir, Path snapshotDir) {
        this.outputDir = outputDir.toAbsolutePath();
        this.snapshotDir = snapshotDir.toAbsolutePath();
    }

    public static DynamicNode compareDirectories(Path outputDir, Path snapshotDir, String displayName) {
        if (!Files.isDirectory(snapshotDir)) {
            return dynamicTest(outputDir.toString(), () -> fail("Missing snapshots " + outputDir));
        }
        final DiffNode diffNode = new DiffNode(outputDir, snapshotDir);
        try {
            return diffNode.createNode(displayName);
        } catch (IOException e) {
            return dynamicTest(displayName, () -> assertNull(e));
        }
    }

    private DynamicNode createNode(String displayName) throws IOException {
        var allFiles = new TreeSet<Path>();

        try (var walk = Files.walk(outputDir)) {
            walk.filter(this::isTextOutput).map(outputDir::relativize).forEach(allFiles::add);
        }
        try (var walk = Files.walk(snapshotDir)) {
            walk.filter(this::isTextOutput).map(snapshotDir::relativize).forEach(allFiles::add);
        }
        allFiles.removeIf(this::isIgnored);

        return dynamicContainer(displayName, allFiles.stream().map(this::compareFile));
    }

    private DynamicNode compareFile(Path path) {
        if (!Files.isRegularFile(outputDir.resolve(path))) {
            return dynamicTest(path.toString(), () -> fail("Missing " + outputDir.resolve(path)));
        }
        if (!Files.isRegularFile(snapshotDir.resolve(path))) {
            return dynamicTest(path.toString(), () -> fail("Missing " + snapshotDir.resolve(path)));
        }
        return dynamicTest(
                path.toString(),
                () -> diff(outputDir.resolve(path), snapshotDir.resolve(path))
        );
    }

    private boolean isIgnored(Path path) {
        var ignoredFiles = Set.of("node_modules", "package-lock.json", "dist", ".gitignore", "git_push.sh");
        return ignoredFiles.contains(path.toString()) || path.toString().startsWith(".openapi-generator");
    }

    private boolean isTextOutput(Path path) {
        return Files.isRegularFile(path) && !path.toString().endsWith(".jar");
    }

    private void diff(Path file, Path snapshot) throws IOException {
        assertTrue(Files.exists(snapshot), "Missing " + snapshot);
        var diff = DiffUtils.diff(Files.readAllLines(snapshot), Files.readAllLines(file));
        if (!diff.getDeltas().isEmpty()) {
            var significantDiff = diff.getDeltas().stream().filter(delta -> !whitespaceOnly(delta)).toList();
            if (significantDiff.isEmpty()) {
                fail("whitespace difference: " + diff.getDeltas());
            } else {
                fail(
                        significantDiff.size() + " significant differences: \n" + significantDiff.stream().map(d -> printDelta(file, d)).collect(Collectors.joining(""))
                );
            }
        }
    }

    private String printDelta(Path file, Delta<String> delta) {
        return "\t" + file + ":" + delta.getOriginal().getPosition() + ": " + delta + "\n";
    }

    private static boolean whitespaceOnly(Delta<String> delta) {
        if (delta instanceof InsertDelta) {
            return delta.getRevised().getLines().stream().allMatch(s -> s == null || s.trim().isEmpty());
        } else if (delta instanceof DeleteDelta) {
            return delta.getOriginal().getLines().stream().allMatch(s -> s == null || s.trim().isEmpty());
        } else {
            return false;
        }
    }
}

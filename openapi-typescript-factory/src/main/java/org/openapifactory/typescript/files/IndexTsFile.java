package org.openapifactory.typescript.files;

import org.openapifactory.api.FileGenerator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class IndexTsFile implements FileGenerator {

    @Override
    public void generate(Path outputRoot) throws IOException {
        Files.writeString(outputRoot.resolve("index.ts"), """
                
                export * from "./api";
                export * from "./model";"""
        );
    }
}

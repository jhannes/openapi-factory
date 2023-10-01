package org.openapifactory.typescript;

import org.openapifactory.api.FileGenerator;
import org.openapifactory.api.OpenapiFactory;
import org.openapifactory.api.codegen.OpenapiSpec;
import org.openapifactory.typescript.files.ApiTsFile;
import org.openapifactory.typescript.files.BaseTsFile;
import org.openapifactory.typescript.files.IndexTsFile;
import org.openapifactory.typescript.files.ApiTestTsFile;
import org.openapifactory.typescript.files.ModelTestTsFile;
import org.openapifactory.typescript.files.ModelTsFile;
import org.openapifactory.typescript.files.PackageJsonFile;
import org.openapifactory.typescript.files.ReadmeFile;
import org.openapifactory.typescript.files.TsconfigJsonFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class OpenapiTypescriptFactory implements OpenapiFactory {

    private final OpenapiTypescriptSpecParser parser;

    public OpenapiTypescriptFactory() {
        parser = new OpenapiTypescriptSpecParser();
    }

    @Override
    public void generate(Path specPath, Path outputRoot) throws IOException {
        Files.createDirectories(outputRoot);

        var spec = parser.createOpenApiSpec(specPath);
        spec.setName(removeExtension(specPath.getFileName()));

        generateFiles(outputRoot, spec);
    }

    private static void generateFiles(Path outputRoot, OpenapiSpec spec) throws IOException {
        List<FileGenerator> files = List.of(
                new ReadmeFile(spec),
                new PackageJsonFile(spec),
                new TsconfigJsonFile(),
                new BaseTsFile(spec),
                new IndexTsFile(),
                new ApiTsFile(spec),
                new ModelTsFile(spec),
                new ApiTestTsFile(spec),
                new ModelTestTsFile(spec)
        );
        for (var file : files) {
            file.generate(outputRoot);
        }
    }

    private String removeExtension(Path fileName) {
        var pos = fileName.toString().lastIndexOf(".");
        return fileName.toString().substring(0, pos);
    }


}

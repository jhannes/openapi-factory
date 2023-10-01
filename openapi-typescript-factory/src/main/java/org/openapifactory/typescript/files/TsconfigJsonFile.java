package org.openapifactory.typescript.files;

import org.openapifactory.api.FileGenerator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TsconfigJsonFile implements FileGenerator {

    @Override
    public void generate(Path outputRoot) throws IOException {
        Files.writeString(outputRoot.resolve("tsconfig.json"), content());
    }

    private String content() {
        return """
                {
                    "compilerOptions": {
                        "strict": true,
                        "declaration": true,
                        "target": "es2015",
                        "module": "commonjs",
                        "noImplicitAny": true,
                        "outDir": "dist",
                        "rootDir": ".",
                        "lib": ["es2019", "dom"],
                        "typeRoots": ["node_modules/@types"]
                    },
                    "exclude": ["dist", "node_modules"]
                }
                """;
    }
}

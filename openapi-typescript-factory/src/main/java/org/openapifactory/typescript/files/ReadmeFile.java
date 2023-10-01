package org.openapifactory.typescript.files;

import org.openapifactory.api.FileGenerator;
import org.openapifactory.api.codegen.OpenapiSpec;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ReadmeFile implements FileGenerator {
    private final OpenapiSpec spec;

    public ReadmeFile(OpenapiSpec spec) {
        this.spec = spec;
    }

    @Override
    public void generate(Path outputRoot) throws IOException {
        Files.writeString(outputRoot.resolve("README.md"), content());
    }

    private String content() {
        return """
                ## %s@%s
                                
                %s
                                
                %s
                                
                ### Environment
                                
                This generator creates TypeScript/JavaScript client. The generated Node module can be used in the following environments:
                                
                Environment
                * Parcel
                                
                Language level
                * ES6
                                
                Module system
                * CommonJS
                * ES6 module system
                                
                It can be used in both TypeScript and JavaScript. In TypeScript, the definition should be automatically resolved via `package.json`. ([Reference](http://www.typescriptlang.org/docs/handbook/typings-for-npm-packages.html))
                                
                ### Building
                                
                To build and compile the typescript sources to javascript use:
                ```
                npm install
                npm run build
                ```
                                
                ### Publishing
                                
                First build the package then run ```npm publish```
                                
                ### Consuming
                                
                navigate to the folder of your consuming project and run one of the following commands.
                                
                _published:_
                                
                ```
                npm install %s@%s --save
                ```
                                
                _unPublished (not recommended):_
                                
                ```
                npm install PATH_TO_GENERATED_PACKAGE --save
                """.formatted(spec.getTitle(), spec.getVersion(), spec.getContact().map(c -> "Contact: " + c.getEmail()).orElse(""), spec.getDescription(), spec.getName(), spec.getVersion());
    }
}

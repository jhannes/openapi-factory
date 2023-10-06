package org.openapifactory.typescript.files;

import org.openapifactory.api.FileGenerator;
import org.openapifactory.api.codegen.CodegenApi;
import org.openapifactory.api.codegen.OpenapiSpec;
import org.openapifactory.typescript.TypescriptFragments;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.openapifactory.api.StringUtil.join;
import static org.openapifactory.api.StringUtil.lines;
import static org.openapifactory.api.StringUtil.toLowerCamelCase;
import static org.openapifactory.typescript.TypescriptFragments.getTypeName;

public class ApiTestTsFile implements FileGenerator {
    private final OpenapiSpec spec;

    public ApiTestTsFile(OpenapiSpec spec) {
        this.spec = spec;
    }

    @Override
    public void generate(Path outputRoot) throws IOException {
        Files.createDirectories(outputRoot.resolve("test"));
        Files.writeString(outputRoot.resolve("test/apiTest.ts"), content());
    }

    private String content() {
        return String.join("", List.of(
                eslintSection(),
                TypescriptFragments.documentationSection(spec),
                importSection(),
                helperSection(),
                apiListSection(),
                apiSection()
        ));
    }

    private String eslintSection() {
        return "/* eslint @typescript-eslint/no-unused-vars: off */\n";
    }

    private String importSection() {
        return ("\n" +
                "import {\n" +
                lines(spec.getModels(), m -> getTypeName(m) + ",").indent(4) +
                "} from \"../model\";\n" +
                "\n" +
                "import {\n" +
                "    ApplicationApis,\n" +
                lines(spec.getApis(), m -> m.getApiName() + "Interface" + ",").indent(4) +
                "} from \"../api\";\n");
    }

    private String helperSection() {
        return """

                function reject(operation: string) {
                    return () => Promise.reject(new Error("Unexpected function call " + operation));
                }
                """;
    }

    private String apiListSection() {
        return "\n" +
               "export function mockApplicationApis({\n" +
               lines(spec.getApis(), a -> toLowerCamelCase(a.getApiName()) + " = mock" + a.getApiName() + "(),").indent(4) +
               "}: Partial<ApplicationApis> = {}): ApplicationApis {\n" +
               "    return { " + join(", ", spec.getApis(), a -> toLowerCamelCase(a.getApiName())) + " };\n" +
               "}\n";
    }

    private String apiSection() {
        return join("", spec.getApis(), this::mockApiSection);
    }

    private String mockApiSection(CodegenApi api) {
        var apiName = api.getApiName();
        return "\n" +
               "export function mock" + apiName + "(\n" +
               "    operations: Partial<" + apiName + "Interface> = {}\n" +
               "): " + apiName + "Interface {\n" +
               "    return {\n" +
               lines(api.getOperations(),
                       o -> o.getOperationId() + ": operations." + o.getOperationId() + " || reject(\"" + apiName + "." + o.getOperationId() + "\"),"
               ).indent(8) +
               "    };\n" +
               "}\n";
    }
}

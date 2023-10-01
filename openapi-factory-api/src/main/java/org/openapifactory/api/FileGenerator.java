package org.openapifactory.api;

import java.io.IOException;
import java.nio.file.Path;

public interface FileGenerator {

    void generate(Path outputRoot) throws IOException;
}

package org.openapifactory.api;

import java.io.IOException;
import java.nio.file.Path;

public interface OpenapiFactory {
    void generate(Path spec, Path outputRoot) throws IOException;
}

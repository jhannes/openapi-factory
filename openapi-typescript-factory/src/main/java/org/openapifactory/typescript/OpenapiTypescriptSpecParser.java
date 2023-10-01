package org.openapifactory.typescript;

import org.openapifactory.api.OpenapiSpecParser;
import org.openapifactory.api.codegen.CodegenArrayType;
import org.openapifactory.api.codegen.CodegenContact;
import org.openapifactory.api.codegen.CodegenContent;
import org.openapifactory.api.codegen.CodegenEnumModel;
import org.openapifactory.api.codegen.CodegenInlineEnumType;
import org.openapifactory.api.codegen.CodegenGenericModel;
import org.openapifactory.api.codegen.CodegenInlineObjectType;
import org.openapifactory.api.codegen.CodegenModel;
import org.openapifactory.api.codegen.CodegenOneOfModel;
import org.openapifactory.api.codegen.CodegenOperation;
import org.openapifactory.api.codegen.CodegenParameter;
import org.openapifactory.api.codegen.CodegenPrimitiveType;
import org.openapifactory.api.codegen.CodegenProp;
import org.openapifactory.api.codegen.CodegenProperty;
import org.openapifactory.api.codegen.CodegenServer;
import org.openapifactory.api.codegen.CodegenType;
import org.openapifactory.api.codegen.CodegenTypeRef;
import org.openapifactory.api.codegen.OpenapiSpec;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.composer.Composer;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.parser.ParserImpl;
import org.yaml.snakeyaml.reader.StreamReader;
import org.yaml.snakeyaml.resolver.Resolver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static org.openapifactory.api.StringUtil.toUpperCamelCase;

public class OpenapiTypescriptSpecParser extends OpenapiSpecParser {

}

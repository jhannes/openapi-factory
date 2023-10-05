package org.openapifactory.typescript.files;

import org.openapifactory.api.FileGenerator;
import org.openapifactory.api.codegen.CodegenAllOfModel;
import org.openapifactory.api.codegen.CodegenArrayType;
import org.openapifactory.api.codegen.CodegenConstantType;
import org.openapifactory.api.codegen.CodegenEnumModel;
import org.openapifactory.api.codegen.CodegenGenericModel;
import org.openapifactory.api.codegen.CodegenInlineEnumType;
import org.openapifactory.api.codegen.CodegenModel;
import org.openapifactory.api.codegen.CodegenOneOfModel;
import org.openapifactory.api.codegen.CodegenPrimitiveType;
import org.openapifactory.api.codegen.CodegenProperty;
import org.openapifactory.api.codegen.CodegenRecordType;
import org.openapifactory.api.codegen.OpenapiSpec;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.openapifactory.api.StringUtil.indent;
import static org.openapifactory.api.StringUtil.join;
import static org.openapifactory.api.StringUtil.lines;
import static org.openapifactory.api.StringUtil.toUpperCamelCase;
import static org.openapifactory.typescript.TypescriptFragments.getTypeName;

public class ModelTestTsFile implements FileGenerator {
    private final OpenapiSpec spec;

    public ModelTestTsFile(OpenapiSpec spec) {
        this.spec = spec;
    }

    @Override
    public void generate(Path outputRoot) throws IOException {
        Files.createDirectories(outputRoot.resolve("test"));
        Files.writeString(outputRoot.resolve("test/modelTest.ts"), content());
    }

    private String content() {
        return String.join("", List.of(
                importSection(),
                helperSection(),
                typesSection(),
                sampleDataClass()
        ));
    }

    private String importSection() {
        return "import {\n" +
               lines(spec.getModels(), m ->
                       m.getName() + "," + (m instanceof CodegenEnumModel ? "\n" + m.getName() + "Values," : "")
                       + importInlineEnumValues(m)
               ).indent(4) + "} from \"../model\";\n\n";
    }

    private String importInlineEnumValues(CodegenModel model) {
        if (model instanceof CodegenGenericModel generic) {
            return generic.getProperties().values().stream()
                    .map(CodegenProperty::getType)
                    .filter(p -> p instanceof CodegenInlineEnumType)
                    .map(t -> ((CodegenInlineEnumType) t))
                    .map(t -> "\n" + t.getName() + "Values,")
                    .collect(Collectors.joining(""));
        } else if (model instanceof CodegenAllOfModel allOf) {
            return allOf.getOwnProperties().stream()
                    .map(CodegenProperty::getType)
                    .filter(p -> p instanceof CodegenInlineEnumType)
                    .map(t -> ((CodegenInlineEnumType) t))
                    .map(t -> "\n" + t.getName() + "Values,")
                    .collect(Collectors.joining(""));
        } else {
            return "";
        }
    }

    private String helperSection() {
        return """
                export class Random {
                    seed: number;
                    constructor(seed: number | string) {
                        this.seed = this.hash(seed) % 2147483647;
                        if (this.seed <= 0) this.seed += 2147483646;
                    }
                                
                    next(): number {
                        this.seed = (this.seed * 16807) % 2147483647;
                        return this.seed;
                    }
                                
                    nextFloat(): number {
                        return (this.next() - 1) / 2147483646;
                    }
                                
                    nextInt(limit: number): number {
                        return this.next() % limit;
                    }
                                
                    nextnumber(limit: number): number {
                        return this.next() % limit;
                    }
                                
                    nextBoolean(): boolean {
                        return this.nextInt(2) == 0;
                    }
                                
                    pickOne<T>(options: readonly T[]): T {
                        return options[this.nextInt(options.length)];
                    }
                                
                    pickSome<T>(options: readonly T[], n?: number): T[] {
                        const shuffled = [...options].sort(() => 0.5 - this.next());
                        return shuffled.slice(0, n || this.nextInt(options.length));
                    }
                                
                    uuidv4(): string {
                        return "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, (c) => {
                            const r = this.nextInt(16) | 0;
                            const v = c == "x" ? r : (r & 0x3) | 0x8;
                            return v.toString(16);
                        });
                    }
                                
                    hash(seed: string | number): number {
                        if (typeof seed === "number") {
                            return seed < 0 ? Math.floor(seed*1000) : Math.floor(seed);
                        }
                        return seed.split("").reduce((a, b) => {
                            a = (a << 5) - a + b.charCodeAt(0);
                            return a & a;
                        }, 0);
                    }
                }

                """;

    }

    private String typesSection() {
        return """
                export type Factory<T> = {
                    [P in keyof T]?: ((sampleData: TestSampleData) => T[P]) | T[P];
                };
                                
                type ModelFactory<T> = Factory<T> | ((testData: TestSampleData) => T);
                                
                export interface SampleModelFactories {
                %s}
                                
                export interface SamplePropertyValues {
                    // eslint-disable-next-line @typescript-eslint/no-explicit-any
                    [key: string]: (sampleData: TestSampleData) => any;
                }
                                
                export interface TestData {
                    seed?: number | string;
                    sampleModelProperties?: SampleModelFactories;
                    samplePropertyValues?: SamplePropertyValues;
                    now?: Date;
                }

                export interface PropertyDefinition {
                    containerClass: string;
                    propertyName: string;
                    // eslint-disable-next-line @typescript-eslint/no-explicit-any
                    example?: string | null | Array<any>;
                    isNullable?: boolean;
                }

                """.formatted(indent(4, spec.getModels(), m -> m.getName() + "?: ModelFactory<" + m.getName() + ">;\n"));

    }

    private String sampleDataClass() {
        return "export class TestSampleData {\n" +
               testSampleDataHelpers() +
               "\n" +
               allModelsFactoryFunction().indent(4) +
               modelFactoryFunctions() +
               "}\n";
    }

    private String testSampleDataHelpers() {
        return """
                    random: Random;
                    // eslint-disable-next-line @typescript-eslint/no-explicit-any
                    sampleModelProperties: any;
                    samplePropertyValues: SamplePropertyValues;
                    now: Date;
                                
                    constructor({ seed, sampleModelProperties, samplePropertyValues, now }: TestData) {
                        this.random = new Random(seed || 100);
                        this.now = now || new Date(2019, 1, this.random.nextInt(2000));
                        this.sampleModelProperties = sampleModelProperties || {};
                        this.samplePropertyValues = samplePropertyValues || {};
                    }
                    
                    nextFloat(): number {
                        return this.random.nextFloat();
                    }
                    
                    nextInt(limit: number): number {
                        return this.random.nextInt(limit);
                    }
                    
                    nextBoolean(): boolean {
                        return this.random.nextBoolean();
                    }
                    
                    sampleboolean(): boolean {
                        return this.random.nextBoolean();
                    }
                    
                    pickOne<T>(options: readonly T[]): T {
                        return this.random.pickOne(options);
                    }
                    
                    pickOneString<T extends string>(options: readonly T[]): T {
                        return this.random.pickOne(options);
                    }
                    
                    pickSome<T>(options: readonly T[]): T[] {
                        return this.random.pickSome(options);
                    }
                    
                    uuidv4(): string {
                        return this.random.uuidv4();
                    }
                                
                    randomString(): string {
                        return this.pickOne(["foo", "bar", "baz"]);
                    }
                                
                    randomArray<T>(generator: (n: number) => T, length?: number): readonly T[] {
                        if (!length) length = this.nextInt(3) + 1;
                        return Array.from({ length }).map((_, index) => generator(index));
                    }
                                
                    randomEmail(): string {
                        return (
                            this.randomFirstName().toLowerCase() +
                            "." +
                            this.randomLastName().toLowerCase() +
                            "@" +
                            this.randomDomain()
                        );
                    }
                                
                    randomFirstName(): string {
                        return this.pickOne(["James", "Mary", "John", "Patricia", "Robert", "Jennifer", "Linda"]);
                    }
                                
                    randomLastName(): string {
                        return this.pickOne(["Smith", "Williams", "Johnson", "Jones", "Brown", "Davis", "Wilson"]);
                    }
                                
                    randomFullName(): string {
                        return this.randomFirstName() + " " + this.randomLastName();
                    }
                                
                    randomDomain(): string {
                        return (
                            this.pickOne(["a", "b", "c", "d", "e"]) +
                            ".example." +
                            this.pickOne(["net", "com", "org"])
                        );
                    }
                                
                    randomPastDateTime(now: Date): Date {
                        return new Date(now.getTime() - this.nextInt(4 * 7 * 24 * 60 * 60 * 1000));
                    }
                                
                    sampleDateTime(): Date {
                        return this.randomPastDateTime(this.now);
                    }
                                
                    samplenumber(): number {
                        return this.nextInt(10000);
                    }
                                
                    sampleunknown(): unknown {
                        return {
                            [this.randomString()]: this.randomString(),
                        }
                    }
                                
                    sampleDate(): Date {
                        return this.randomPastDateTime(this.now);
                    }
                                
                    sampleString(dataFormat?: string, example?: string): string {
                        if (dataFormat === "uuid") {
                            return this.uuidv4();
                        }
                        if (dataFormat === "uri") {
                            return "https://" + this.randomDomain() + "/" + this.randomFirstName().toLowerCase();
                        }
                        if (dataFormat === "email") {
                            return this.randomEmail();
                        }
                        if (example && example !== "null") return example;
                        return this.randomString();
                    }
                                
                    sampleArrayString(length?: number): Array<string> {
                        return Array.from({ length: length || this.arrayLength() }).map(() => this.sampleString());
                    }
                                
                    // eslint-disable-next-line @typescript-eslint/no-unused-vars
                    sampleArrayArray<T>(length?: number): readonly T[] {
                        return [];
                    }
                                
                    sampleArraynumber(length?: number): Array<number> {
                        return Array.from({ length: length || this.arrayLength() }).map(() => this.samplenumber());
                    }
                                
                    generate(
                        // eslint-disable-next-line @typescript-eslint/no-explicit-any
                        template?: ((sampleData: TestSampleData) => any) | any,
                        propertyDefinition?: PropertyDefinition,
                        // eslint-disable-next-line @typescript-eslint/no-explicit-any
                        generator?: () => any
                        // eslint-disable-next-line @typescript-eslint/no-explicit-any
                    ): any {
                        if (template != undefined) {
                            return typeof template === "function" ? template(this) : template;
                        }
                        if (propertyDefinition) {
                            const { containerClass, propertyName, example } = propertyDefinition;
                            if (this.sampleModelProperties[containerClass]) {
                                const propertyFactory = this.sampleModelProperties[containerClass][propertyName];
                                if (propertyFactory && typeof propertyFactory === "function") {
                                    return propertyFactory(this);
                                } else if (propertyFactory !== undefined) {
                                    return propertyFactory;
                                }
                            }
                            if (this.samplePropertyValues[propertyName] !== undefined) {
                                return this.samplePropertyValues[propertyName](this);
                            }
                            if (example && example !== "null") return example;
                        }
                        return generator && generator();
                    }
                                
                    arrayLength(): number {
                        return this.nextInt(3) + 1;
                    }
                """;
    }

    private String allModelsFactoryFunction() {
        return "// eslint-disable-next-line @typescript-eslint/no-explicit-any\n" +
               "sample(modelName: string): any {\n" +
               "    switch (modelName) {\n" +
               indent(8, spec.getModels(), m ->
                       "case \"" + m.getName() + "\":\n" +
                       "    return this.sample" + m.getName() + "();\n" +
                       "case \"Array<" + m.getName() + ">\":\n" +
                       "    return this.sampleArray" + m.getName() + "();\n"
               ) +
               "        default:\n" +
               "            throw new Error(\"Unknown type \" + modelName);\n" +
               "    }\n" +
               "}\n";
    }

    private String modelFactoryFunctions() {
        return join(spec.getModels(), this::singleModelFactoryFunctions);
    }

    private String singleModelFactoryFunctions(CodegenModel model) {
        if (model instanceof CodegenGenericModel generic) {
            var type = model.getName();
            return "\n" +
                   ("sample" + type + "(template?: Factory<" + type + ">): " + type + " {\n" +
                    "    const containerClass = \"" + type + "\";\n" +
                    "    if (!template && typeof this.sampleModelProperties[containerClass] === \"function\") {\n" +
                    "        return this.sampleModelProperties[containerClass](this);\n" +
                    "    }\n" +
                    "    return {\n" +
                    indent(8, generic.getProperties().values(), ModelTestTsFile::propertyFactory) +
                    "    };\n" +
                    "}").indent(4) +
                   "\n" + modelArrayFactory(model).indent(4);
        } else if (model instanceof CodegenEnumModel enumModel) {
            var type = model.getName();
            return "\n" +
                   ("sample" + type + "(): " + type + " {\n" +
                    "    const containerClass = \"" + type + "\";\n" +
                    "    if (typeof this.sampleModelProperties[containerClass] === \"function\") {\n" +
                    "        return this.sampleModelProperties[containerClass](this);\n" +
                    "    }\n" +
                    "    return this.pickOne(" + type + "Values);\n" +
                    "}").indent(4) +
                   "\n" +
                   singleArrayFactory(type).indent(4);
        } else if (model instanceof CodegenOneOfModel oneOf) {
            var type = model.getName();
            var name = model.getName();
            return "\n" +
                   ("sample" + type + "(\n" +
                    "    factory?: (sampleData: TestSampleData) => " + type + "\n" +
                    "): " + type + " {\n" +
                    "    const containerClass = \"" + type + "\";\n" +
                    "    if (factory) {\n" +
                    "        return factory(this);\n" +
                    "    }\n" +
                    "    if (typeof this.sampleModelProperties[containerClass] === \"function\") {\n" +
                    "        return this.sampleModelProperties[containerClass](this);\n" +
                    "    }\n" +
                    pickOneFromOneOf(oneOf) +
                    "}").indent(4) +
                   "\n" +
                   ("sampleArray" + name + "(\n" +
                    "    length?: number,\n" +
                    "    factory?: (sampleData: TestSampleData) => " + type + "\n" +
                    "): readonly " + name + "[] {\n" +
                    "    return this.randomArray(\n" +
                    "        () => this.sample" + name + "(factory),\n" +
                    "        length ?? this.arrayLength()\n" +
                    "    );\n" +
                    "}").indent(4);
        } else if (model instanceof CodegenAllOfModel allOf) {
            return "\n" +
                   (
                           "sample" + model.getName() + "(template?: Factory<" + model.getName() + ">): " + model.getName() + " {\n" +
                           "    const containerClass = \"" + model.getName() + "\";\n" +
                           "    if (!template && typeof this.sampleModelProperties[containerClass] === \"function\") {\n" +
                           "        return this.sampleModelProperties[containerClass](this);\n" +
                           "    }\n" +
                           "    return {\n" +
                           indent(8, allOf.getRefSuperModels(), s -> "...this.sample" + s.getClassName() + "Dto(template),\n") +
                           indent(8, allOf.getOwnProperties(), ModelTestTsFile::propertyFactory) +
                           "    };\n" +
                           "}\n").indent(4) +
                   "\n" +
                   modelArrayFactory(model).indent(4);
        } else {
            throw new RuntimeException("Unhandled " + model);
        }
    }

    private String pickOneFromOneOf(CodegenOneOfModel oneOf) {
        var discriminator = oneOf.getDiscriminator().getPropertyName();
        if (discriminator == null) {
            return "    return this.pickOne([\n" +
                   indent(8, oneOf.getOneOf(), s -> "() => this.sample" + getTypeName(s) + "(),\n") +
                   "    ])();\n";
        }
        return "    const " + discriminator + " = this.pickOneString([" +
               join(", ", oneOf.getMappedModels(), s -> "\"" + s.getName() + "\"") + "])\n" +
               "    switch (" + discriminator + ") {\n" +
               indent(8, oneOf.getMappedModels(), mapped ->
                       "case \"" + mapped.getName() + "\":\n" +
                       (mapped.getType() instanceof CodegenOneOfModel
                               ? "    return this.sample" + mapped.getType().getName() + "();\n"
                               : (
                               "    return {\n" +
                               "        ...this.sample" + (mapped.getType().getName()) + "(),\n" +
                               "        " + discriminator + ",\n" +
                               "    };\n")
                       )
               ) +
               "    }\n";
    }

    private static String singleArrayFactory(String type) {
        return "sampleArray" + type + "(length?: number): readonly " + type + "[] {\n" +
               "    return this.randomArray(\n" +
               "        () => this.sample" + type + "(),\n" +
               "        length ?? this.arrayLength()\n" +
               "    );\n" +
               "}";
    }

    private static String propertyFactory(CodegenProperty p) {
        if (p.getType() instanceof CodegenPrimitiveType primitive) {
            if ("date".equals(primitive.getFormat()) || "date-time".equals(primitive.getFormat())) {
                return p.getName() + ": this.generate(\n" +
                       "    template?." + p.getName() + ",\n" +
                       "    { containerClass, propertyName: \"" + p.getName() + "\", example: \"null\", isNullable: false },\n" +
                       "    () => this.sampleDate()\n" +
                       "),\n";
            }
            if (primitive.getType().equals("string")) {
                return p.getName() + ": this.generate(\n" +
                       "    template?." + p.getName() + ",\n" +
                       "    { containerClass, propertyName: \"" + p.getName() + "\", isNullable: false },\n" +
                       "    () => this.sampleString(\"" + Objects.toString(primitive.getFormat(), "") + "\", \"" + p.getExample() + "\")\n" +
                       "),\n";
            }
        } else if (p.getType() instanceof CodegenConstantType constant) {
            return p.getName() + ": \"" + constant.getValue() + "\",\n";
        } else if (p.getType() instanceof CodegenInlineEnumType enumModel) {
            return p.getName() + ": this.generate(\n" +
                   "    template?." + p.getName() + ",\n" +
                   "    { containerClass, propertyName: \"" + p.getName() + "\", example: \"null\", isNullable: false },\n" +
                   "    () => this.pickOne(" + enumModel.getName() + "Values)\n" +
                   "),\n";
        } else if (p.getType() instanceof CodegenArrayType array) {
            var functionCall = "sampleArray" + toUpperCamelCase(getTypeName(array.getItems()));
            if (array.getItems() instanceof CodegenInlineEnumType || array.getItems() instanceof CodegenConstantType) {
                functionCall = "sampleArrayString";
            }
            return p.getName() + ": this.generate(\n" +
                   "    template?." + p.getName() + ",\n" +
                   "    { containerClass, propertyName: \"" + p.getName() + "\", example: null, isNullable: false },\n" +
                   "    () => this." + functionCall + "()\n" +
                   "),\n";
        } else if (p.getType() instanceof CodegenRecordType record) {
            // TODO: This is a bug in the old generator - records shouldn't generate arrays
            var functionCall = "sampleArray" + toUpperCamelCase(getTypeName(record.getAdditionalProperties()));
            if (record.getAdditionalProperties() instanceof CodegenInlineEnumType || record.getAdditionalProperties() instanceof CodegenConstantType) {
                functionCall = "sampleArrayString";
            }
            return p.getName() + ": this.generate(\n" +
                   "    template?." + p.getName() + ",\n" +
                   "    { containerClass, propertyName: \"" + p.getName() + "\", example: null, isNullable: false },\n" +
                   "    () => this." + functionCall + "()\n" +
                   "),\n";
        }
        return p.getName() + ": this.generate(\n" +
               "    template?." + p.getName() + ",\n" +
               "    { containerClass, propertyName: \"" + p.getName() + "\", example: \"null\", isNullable: false },\n" +
               "    () => this.sample" + getTypeName(p.getType()) + "()\n" +
               "),\n";
    }

    private static String modelArrayFactory(CodegenModel model) {
        var name = model.getName();
        return ("sampleArray" + name + "(\n" +
                "    length?: number,\n" +
                "    template?: Factory<" + name + ">\n" +
                "): readonly " + name + "[] {\n" +
                "    return this.randomArray(\n" +
                "        () => this.sample" + name + "(template),\n" +
                "        length ?? this.arrayLength()\n" +
                "    );\n" +
                "}");
    }
}

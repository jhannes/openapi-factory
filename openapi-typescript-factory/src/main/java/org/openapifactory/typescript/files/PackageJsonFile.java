package org.openapifactory.typescript.files;

import org.openapifactory.api.FileGenerator;
import org.openapifactory.api.codegen.OpenapiSpec;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class PackageJsonFile implements FileGenerator {
    private final OpenapiSpec spec;

    public PackageJsonFile(OpenapiSpec spec) {
        this.spec = spec;
    }

    @Override
    public void generate(Path outputRoot) throws IOException {
        Files.writeString(outputRoot.resolve("package.json"), content());
    }

    private String content() {
        return """
                {
                    "name": "%s",
                    "version": "%s",
                    "description": "OpenAPI client for %s",
                    "author": "OpenAPI-Generator Contributors",
                    "keywords": [
                        "typescript",
                        "openapi-client",
                        "openapi-generator",
                        "%s"
                    ],
                    "files": [
                        "dist"
                    ],
                    "main": "./dist/index.js",
                    "typings": "./dist/index.d.ts",
                    "scripts": {
                        "build": "npm run lint && npm run build:typescript",
                        "build:typescript": "tsc --outDir dist/",
                        "lint": "eslint . --ext .ts",
                        "format": "prettier --write *.ts test/*.ts *.json",
                        "verify": "npm run lint && tsc --noEmit",
                        "prepublishOnly": "npm run build"
                    },
                    "devDependencies": {
                        "typescript": "^4.7.4",
                        "eslint": "^8.20.0",
                        "prettier": "^2.7.1",
                        "@typescript-eslint/eslint-plugin": "^5.30.7",
                        "@typescript-eslint/parser": "^5.30.7",
                        "eslint-config-prettier": "^8.5.0",
                        "eslint-plugin-prettier": "^4.2.1"
                    },
                    "eslintConfig": {
                        "root": true,
                        "parser": "@typescript-eslint/parser",
                        "plugins": [
                            "@typescript-eslint"
                        ],
                        "extends": [
                            "eslint:recommended",
                            "plugin:@typescript-eslint/eslint-recommended",
                            "plugin:@typescript-eslint/recommended",
                            "prettier"
                        ],
                        "rules": {
                            "comma-dangle": [
                                "warn",
                                "always-multiline"
                            ],
                            "indent": [
                                "error",
                                4,
                                {
                                    "SwitchCase": 1
                                }
                            ],
                            "linebreak-style": [
                                "error",
                                "unix"
                            ],
                            "max-len": [
                                "warn",
                                100,
                                {
                                    "comments": 140,
                                    "ignorePattern": "\\\\{ containerClass, propertyName:|sample\\\\w+Dto\\\\(template\\\\?: Factory|operation\\\\.\\\\w || reject"
                                }
                            ],
                            "no-trailing-spaces": "error",
                            "quotes": "warn",
                            "@typescript-eslint/explicit-function-return-type": [
                                "warn",
                                {
                                    "allowExpressions": true
                                }
                            ]
                        },
                        "ignorePatterns": [
                            "dist/**"
                        ]
                    },
                    "prettier": {
                        "tabWidth": 4,
                        "printWidth": 100
                    }
                }
                """.formatted(spec.getName(), spec.getVersion(), spec.getName(), spec.getName());
    }
}

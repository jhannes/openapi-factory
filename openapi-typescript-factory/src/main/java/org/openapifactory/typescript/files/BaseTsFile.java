package org.openapifactory.typescript.files;

import org.openapifactory.api.FileGenerator;
import org.openapifactory.api.codegen.OpenapiSpec;
import org.openapifactory.typescript.TypescriptFragments;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class BaseTsFile implements FileGenerator {
    private final OpenapiSpec spec;

    public BaseTsFile(OpenapiSpec spec) {
        this.spec = spec;
    }

    @Override
    public void generate(Path outputRoot) throws IOException {
        Files.writeString(outputRoot.resolve("base.ts"),
                """
                /* eslint @typescript-eslint/no-explicit-any: off */
                                
                /* eslint @typescript-eslint/explicit-module-boundary-types: off */
                %s
                export interface RequestOptions {
                    mode?: RequestMode;
                    headers?: Record<string, string>;
                    cache?: RequestCache;
                    credentials?: RequestCredentials;
                    referrer?: string;
                    referrerPolicy?: ReferrerPolicy;
                }
                                
                export type RequestCallOptions = RequestOptions & {
                    signal?: AbortSignal | null;
                };
                                
                export class BaseAPI {
                    readonly basePath: string;
                    readonly requestOptions?: RequestOptions;
                                
                    constructor(
                        basePath: string = window.location.origin,
                        requestOptions?: RequestOptions
                    ) {
                        this.basePath = basePath;
                        this.requestOptions = requestOptions;
                    }
                                
                    protected async fetch(
                        url: string,
                        options: RequestCallOptions & { method?: string; body?: string } = {}
                    ): Promise<any> {
                        const result = await fetch(url, {
                            credentials: options.credentials || this.requestOptions?.credentials || "same-origin",
                            mode: options.mode || this.requestOptions?.mode,
                            method: options.method,
                            body: options.body,
                            cache: options.cache || this.requestOptions?.cache,
                            referrer: options.referrer || this.requestOptions?.referrer,
                            referrerPolicy: options.referrerPolicy || this.requestOptions?.referrerPolicy,
                            signal: options.signal,
                            headers: {
                                ...(this.requestOptions?.headers || {}),
                                ...options.headers,
                            },
                        });
                        return await this.handleResponse(result);
                    }
                                
                    protected formData(form: any): string {
                        return Object.keys(form)
                            .map((key) => key + "=" + encodeURIComponent(form[key]))
                            .join("&");
                    }
                                
                    protected async handleResponse(response: Response): Promise<any> {
                        const contentType = response.headers.get("Content-type");
                        if (response.redirected) {
                            window.location.href = response.url;
                            throw new RedirectedError(response);
                        }
                        if (response.ok) {
                            if (contentType && contentType.startsWith("application/json")) {
                                return response.json();
                            }
                            if (response.status === 204) {
                                return undefined;
                            }
                            return response;
                        }
                        const body: any =
                            contentType && contentType.startsWith("application/json") && (await response.json());
                        if (response.status == 401) {
                            throw new LoggedOutError(response, body);
                        } else if (response.status == 404) {
                            throw new NotFoundError(response, body);
                        } else if (response.status >= 500) {
                            throw new ServerError(response, body);
                        } else {
                            throw new RequestError(response, body);
                        }
                    }
                                
                    protected url(
                        pathTemplate: string,
                        params: any,
                        queryParams?: QueryParams,
                        queryOptions?: QueryOptions
                    ): string {
                        return (
                            this.basePath +
                            this.expandPathTemplate(pathTemplate, params) +
                            this.query(queryParams || {}, queryOptions || {})
                        );
                    }
                                
                    protected removeEmpty(obj: Record<string, string | undefined> = {}): Record<string, string> {
                        return Object.keys(obj)
                            .filter((k) => obj[k] != null)
                            .reduce((a, k) => ({ ...a, [k]: obj[k] }), {}) as Record<string, string>;
                    }
                                
                    private expandPathTemplate(pathTemplate: string, params: any): string {
                        return pathTemplate.replace(/{(\\w+)}/g, (match, g) => params[g]);
                    }
                                
                    protected query(queryParams: QueryParams, queryOptions: QueryOptions): string {
                        if (!Object.keys(queryParams).length) {
                            return "";
                        }
                        const query = new URLSearchParams();
                        for (const key of Object.keys(queryParams)) {
                            const value = queryParams[key];
                            const options = queryOptions[key];
                            if (Array.isArray(value)) {
                                if (!options?.explode) {
                                    for (const item of value) {
                                        if (item instanceof Date) {
                                            query.append(
                                                key,
                                                options?.format === "date"
                                                    ? item.toISOString().substr(0, 10)
                                                    : item.toISOString()
                                            );
                                        } else {
                                            query.append(key, item);
                                        }
                                    }
                                } else if (options?.format === "date") {
                                    query.append(
                                        key,
                                        (value as Array<Date>)
                                            .map((item) => item.toISOString().substr(0, 10))
                                            .join(options.delimiter || ",")
                                    );
                                } else {
                                    query.append(key, value.join(options.delimiter || ","));
                                }
                            } else if (value instanceof Date) {
                                query.append(
                                    key,
                                    options?.format === "date"
                                        ? value.toISOString().substr(0, 10)
                                        : value.toISOString()
                                );
                            } else if (value !== undefined) {
                                query.append(key, value.toString());
                            }
                        }
                        return "?" + query;
                    }
                }
                                
                type QueryParams = Record<string, string | string[] | Date | Date[] | boolean | number | undefined>;
                type QueryOptions = Record<string, { explode?: boolean; delimiter?: "," | " " | "|", format?: "date" }>;
                                
                export class HttpError extends Error {
                    readonly response: Response;
                    readonly body?: any;
                                
                    constructor(response: Response, body?: any) {
                        super(body?.message || response.statusText || response.status);
                        this.response = response;
                        this.body = body;
                        this.name = new.target.name;
                        Object.setPrototypeOf(this, new.target.prototype);
                    }
                }
                                
                export class ServerError extends HttpError {
                    constructor(response: Response, body: any) {
                        super(response, body);
                    }
                }
                                
                export class RequestError extends HttpError {
                    constructor(response: Response, body: any) {
                        super(response, body);
                    }
                }
                                
                export class LoggedOutError extends RequestError {
                    constructor(response: Response, body: any) {
                        super(response, body);
                    }
                }
                                
                export class NotFoundError extends RequestError {
                    constructor(response: Response, body: any) {
                        super(response, body);
                    }
                }
                                
                export class RedirectedError extends HttpError {
                    constructor(response: Response) {
                        super(response);
                    }
                }
                                
                export interface SecurityScheme {
                    headers(): Record<string, string>;
                }
                """.formatted(TypescriptFragments.documentationSection(spec))
        );
    }
}

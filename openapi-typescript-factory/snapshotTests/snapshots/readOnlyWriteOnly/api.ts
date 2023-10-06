/* eslint @typescript-eslint/no-unused-vars: off */
/**
 * User database
 * User database
 *
 * The version of the OpenAPI document: 1.0.0
 *
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */

import {
    UserDto,
    UserDtoResponse,
    UserDtoRequest,
    UserPermissionDto,
    UserPermissionDtoRequest,
} from "./model";

import { BaseAPI, RequestCallOptions, SecurityScheme } from "./base";

export interface ApplicationApis {
    defaultApi: DefaultApiInterface;
}

/**
 * DefaultApi - object-oriented interface
 */
export interface DefaultApiInterface {
    /**
     *
     * @param {*} [params] Request parameters, including pathParams, queryParams (including bodyParams) and http options.
     * @throws {HttpError}
     */
    usersAllPost(params: {
        userDto: Array<UserDtoRequest>;
    } & RequestCallOptions): Promise<void>;
    /**
     *
     * @throws {HttpError}
     */
    usersGet(params?: RequestCallOptions): Promise<Array<UserDtoResponse>>;
    /**
     *
     * @param {*} [params] Request parameters, including pathParams, queryParams (including bodyParams) and http options.
     * @throws {HttpError}
     */
    usersIdGet(params: {
        pathParams: { id: number, };
    } & RequestCallOptions): Promise<UserDtoResponse>;
    /**
     *
     * @param {*} [params] Request parameters, including pathParams, queryParams (including bodyParams) and http options.
     * @throws {HttpError}
     */
    usersPost(params: {
        userDto: UserDtoRequest;
    } & RequestCallOptions): Promise<void>;
}

/**
 * DefaultApi - object-oriented interface
 */
export class DefaultApi extends BaseAPI implements DefaultApiInterface {
    /**
     *
     * @param {*} [params] Request parameters, including pathParams, queryParams (including bodyParams) and http options.
     * @throws {HttpError}
     */
    public async usersAllPost(params: {
        userDto: Array<UserDtoRequest>;
    } & RequestCallOptions): Promise<void> {
        return await this.fetch(
            this.basePath + "/users/all",
            {
                ...params,
                method: "POST",
                body: JSON.stringify(params.userDto),
                headers: {
                    ...this.removeEmpty(params.headers),
                    "Content-Type": "application/json",
                },
            }
        );
    }
    /**
     *
     * @throws {HttpError}
     */
    public async usersGet(params: RequestCallOptions = {}): Promise<Array<UserDtoResponse>> {
        return await this.fetch(
            this.basePath + "/users", params
        );
    }
    /**
     *
     * @param {*} [params] Request parameters, including pathParams, queryParams (including bodyParams) and http options.
     * @throws {HttpError}
     */
    public async usersIdGet(params: {
        pathParams: { id: number, };
    } & RequestCallOptions): Promise<UserDtoResponse> {
        return await this.fetch(
            this.url("/users/{id}", params.pathParams), params
        );
    }
    /**
     *
     * @param {*} [params] Request parameters, including pathParams, queryParams (including bodyParams) and http options.
     * @throws {HttpError}
     */
    public async usersPost(params: {
        userDto: UserDtoRequest;
    } & RequestCallOptions): Promise<void> {
        return await this.fetch(
            this.basePath + "/users",
            {
                ...params,
                method: "POST",
                body: JSON.stringify(params.userDto),
                headers: {
                    ...this.removeEmpty(params.headers),
                    "Content-Type": "application/json",
                },
            }
        );
    }
}

type ServerNames =
    | "default";

export const servers: Record<ServerNames, ApplicationApis> = {
    default: {
        defaultApi: new DefaultApi(""),
    },
};


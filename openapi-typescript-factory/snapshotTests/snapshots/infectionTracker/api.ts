/* eslint @typescript-eslint/no-unused-vars: off */
/**
 * Infection Tracker
 * Infection Tracker - A case management system for tracking the spread of diseases
 *
 * The version of the OpenAPI document: 1.0.0-draft
 *
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */

import {
    CaseWorkerDto,
    ExposureDto,
    InfectionDto,
    InfectionDtoRequest,
    InfectionInformationDto,
    UserRoleDto,
} from "./model";

import { BaseAPI, RequestCallOptions, SecurityScheme } from "./base";

export interface ApplicationApis {
    caseWorkersApi: CaseWorkersApiInterface;
    casesApi: CasesApiInterface;
    exposuresApi: ExposuresApiInterface;
}

/**
 * CaseWorkersApi - object-oriented interface
 */
export interface CaseWorkersApiInterface {
    /**
     *
     * @throws {HttpError}
     */
    listCaseWorkers(params?: RequestCallOptions): Promise<CaseWorkerDto>;
    /**
     *
     * @param {*} [params] Request parameters, including pathParams, queryParams (including bodyParams) and http options.
     * @throws {HttpError}
     */
    registerCaseWorker(params: {
        caseWorkerDto: CaseWorkerDto;
    } & RequestCallOptions): Promise<void>;
}

/**
 * CaseWorkersApi - object-oriented interface
 */
export class CaseWorkersApi extends BaseAPI implements CaseWorkersApiInterface {
    /**
     *
     * @throws {HttpError}
     */
    public async listCaseWorkers(params: RequestCallOptions = {}): Promise<CaseWorkerDto> {
        return await this.fetch(
            this.basePath + "/api/caseWorkers", params
        );
    }
    /**
     *
     * @param {*} [params] Request parameters, including pathParams, queryParams (including bodyParams) and http options.
     * @throws {HttpError}
     */
    public async registerCaseWorker(params: {
        caseWorkerDto: CaseWorkerDto;
    } & RequestCallOptions): Promise<void> {
        return await this.fetch(
            this.basePath + "/api/caseWorkers",
            {
                ...params,
                method: "POST",
                body: JSON.stringify(params.caseWorkerDto),
                headers: {
                    ...this.removeEmpty(params.headers),
                    "Content-Type": "application/json",
                },
            }
        );
    }
}

/**
 * CasesApi - object-oriented interface
 */
export interface CasesApiInterface {
    /**
     *
     * @param {*} [params] Request parameters, including pathParams, queryParams (including bodyParams) and http options.
     * @throws {HttpError}
     */
    getCaseDetails(params: {
        pathParams: { caseId: string };
    } & RequestCallOptions): Promise<InfectionDto>;
    /**
     *
     * @throws {HttpError}
     */
    listCases(params?: RequestCallOptions): Promise<InfectionDto>;
    /**
     *
     * @param {*} [params] Request parameters, including pathParams, queryParams (including bodyParams) and http options.
     * @throws {HttpError}
     */
    newCase(params?: {
        infectionInformationDto?: InfectionInformationDto;
    } & RequestCallOptions): Promise<void>;
    /**
     *
     * @param {*} [params] Request parameters, including pathParams, queryParams (including bodyParams) and http options.
     * @throws {HttpError}
     */
    registerExposure(params: {
        pathParams: { caseId: string };
        exposureDto?: ExposureDto;
    } & RequestCallOptions): Promise<void>;
}

/**
 * CasesApi - object-oriented interface
 */
export class CasesApi extends BaseAPI implements CasesApiInterface {
    /**
     *
     * @param {*} [params] Request parameters, including pathParams, queryParams (including bodyParams) and http options.
     * @throws {HttpError}
     */
    public async getCaseDetails(params: {
        pathParams: { caseId: string };
    } & RequestCallOptions): Promise<InfectionDto> {
        return await this.fetch(
            this.url("/api/cases/{caseId}", params.pathParams), params
        );
    }
    /**
     *
     * @throws {HttpError}
     */
    public async listCases(params: RequestCallOptions = {}): Promise<InfectionDto> {
        return await this.fetch(
            this.basePath + "/api/cases", params
        );
    }
    /**
     *
     * @param {*} [params] Request parameters, including pathParams, queryParams (including bodyParams) and http options.
     * @throws {HttpError}
     */
    public async newCase(params?: {
        infectionInformationDto?: InfectionInformationDto;
    } & RequestCallOptions): Promise<void> {
        return await this.fetch(
            this.basePath + "/api/cases",
            {
                ...params,
                method: "POST",
                body: params?.infectionInformationDto ? JSON.stringify(params.infectionInformationDto) : undefined,
                headers: {
                    ...this.removeEmpty(params?.headers),
                    "Content-Type": "application/json",
                },
            }
        );
    }
    /**
     *
     * @param {*} [params] Request parameters, including pathParams, queryParams (including bodyParams) and http options.
     * @throws {HttpError}
     */
    public async registerExposure(params: {
        pathParams: { caseId: string };
        exposureDto?: ExposureDto;
    } & RequestCallOptions): Promise<void> {
        return await this.fetch(
            this.url("/api/cases/{caseId}/exposures", params.pathParams),
            {
                ...params,
                method: "POST",
                body: JSON.stringify(params.exposureDto),
                headers: {
                    ...this.removeEmpty(params.headers),
                    "Content-Type": "application/json",
                },
            }
        );
    }
}

/**
 * ExposuresApi - object-oriented interface
 */
export interface ExposuresApiInterface {
    /**
     *
     * @param {*} [params] Request parameters, including pathParams, queryParams (including bodyParams) and http options.
     * @throws {HttpError}
     */
    listExposures(params?: {
        queryParams?: { exposureDate?: Array<Date>; maxCount?: number };
    } & RequestCallOptions): Promise<ExposureDto>;
    /**
     *
     * @param {*} [params] Request parameters, including pathParams, queryParams (including bodyParams) and http options.
     * @throws {HttpError}
     */
    updateExposure(params: {
        pathParams: { exposureId: string };
        exposureDto?: ExposureDto;
    } & RequestCallOptions): Promise<void>;
}

/**
 * ExposuresApi - object-oriented interface
 */
export class ExposuresApi extends BaseAPI implements ExposuresApiInterface {
    /**
     *
     * @param {*} [params] Request parameters, including pathParams, queryParams (including bodyParams) and http options.
     * @throws {HttpError}
     */
    public async listExposures(params?: {
        queryParams?: { exposureDate?: Array<Date>; maxCount?: number };
    } & RequestCallOptions): Promise<ExposureDto> {
        return await this.fetch(
            this.url("/api/exposures", {}, params?.queryParams, {
                exposureDate: { delimiter: "|", explode: false, format: "date" },
            }), params
        );
    }
    /**
     *
     * @param {*} [params] Request parameters, including pathParams, queryParams (including bodyParams) and http options.
     * @throws {HttpError}
     */
    public async updateExposure(params: {
        pathParams: { exposureId: string };
        exposureDto?: ExposureDto;
    } & RequestCallOptions): Promise<void> {
        return await this.fetch(
            this.url("/api/exposures/{exposureId}", params.pathParams),
            {
                ...params,
                method: "PUT",
                body: JSON.stringify(params.exposureDto),
                headers: {
                    ...this.removeEmpty(params.headers),
                    "Content-Type": "application/json",
                },
            }
        );
    }
}

type ServerNames =
    | "current"
    | "production";

export const servers: Record<ServerNames, ApplicationApis> = {
    "current": {
        caseWorkersApi: new CaseWorkersApi("/api"),
        casesApi: new CasesApi("/api"),
        exposuresApi: new ExposuresApi("/api"),
    },
    "production": {
        caseWorkersApi: new CaseWorkersApi("https://infectiontracker.example.gov/api"),
        casesApi: new CasesApi("https://infectiontracker.example.gov/api"),
        exposuresApi: new ExposuresApi("https://infectiontracker.example.gov/api"),
    },
};


package com.example.adventurebook.api;

import com.example.adventurebook.config.TafConfig;
import io.qameta.allure.Step;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.config.LogConfig;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.Filter;
import io.restassured.filter.log.LogDetail;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import java.util.ArrayList;
import java.util.List;

/**
 * The one HTTP entry point. {@link #request()} returns a REST Assured {@link RequestSpecification} that is already
 * configured (base URI, JSON, shared Jackson mapper, REST Assured's standard log, Allure attachments, coverage and - optionally -
 * OpenAPI contract validation), so every endpoint and every verb is simply:
 * <pre>
 *   client.request().pathParam("id", 7).get("/api/books/{id}")        // -> io.restassured.response.Response
 *   client.request().body(dto).post("/api/books")
 * </pre>
 * and the response is a plain {@code Response}: {@code then().statusCode(201)}, {@code as(Foo.class)},
 * {@code as(Foo[].class)}. Instances are immutable and thread-safe.
 */
public final class ApiClient {

    private final String baseUrl;
    private final RestAssuredConfig config;
    private final List<Filter> filters;

    public ApiClient(String baseUrl) {
        this(baseUrl, null);
    }

    private ApiClient(String baseUrl, ContractValidator contract) {
        this.baseUrl = baseUrl;
        String logMode = TafConfig.get().apiLog();

        // REST Assured serialises/deserialises with the shared mapper (unknown fields ignored, nulls omitted)
        RestAssuredConfig restAssured = RestAssuredConfig.config().objectMapperConfig(
                ObjectMapperConfig.objectMapperConfig().jackson2ObjectMapperFactory((type, charset) -> Json.mapper()));
        if (logMode.equals("failures")) {
            restAssured = restAssured.logConfig(LogConfig.logConfig().enableLoggingOfRequestAndResponseIfValidationFails());
        }
        this.config = restAssured;

        // order matters: the LAST filter is closest to the wire, so the response is logged before validation filters run
        List<Filter> chain = new ArrayList<>();
        if (logMode.equals("all")) {
            chain.add(new RequestLoggingFilter(LogDetail.ALL));
        }
        chain.add(new AllureRestAssured());
        chain.add(new ApiCoverageFilter(contract != null)); // before the contract filter: violations are not "covered"
        if (contract != null) {
            chain.add(contract.filter());
        }
        if (logMode.equals("all")) {
            chain.add(new ResponseLoggingFilter(LogDetail.ALL));
        }
        this.filters = List.copyOf(chain);
    }

    /** Same client, but every call is also validated against the OpenAPI document. */
    public ApiClient withContractValidation(ContractValidator validator) {
        return new ApiClient(baseUrl, validator);
    }

    public RequestSpecification request() {
        return RestAssured.given()
                .config(config)
                .baseUri(baseUrl)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .filters(filters);
    }

    // ---------- shared, stateless instances, created on first use ----------

    private static final class Standard {
        static final ApiClient CLIENT = new ApiClient(TafConfig.get().apiBaseUrl());
    }

    private static final class WithContract {
        static final ApiClient CLIENT = Standard.CLIENT.withContractValidation(ContractValidator.fromConfig());
    }

    /** Plain client for functional tests and test preconditions. */
    public static ApiClient standard() {
        return Standard.CLIENT;
    }

    /** Client that additionally validates every request/response against the OpenAPI document. */
    public static ApiClient withContract() {
        return WithContract.CLIENT;
    }
}

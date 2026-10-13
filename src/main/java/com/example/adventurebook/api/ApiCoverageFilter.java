package com.example.adventurebook.api;

import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * Feeds {@link ApiCoverage}: records every finished call as (verb, path template, status). It sits BEFORE the
 * contract filter in the chain, so a call that violates the contract throws first and is never counted as covered.
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
final class ApiCoverageFilter implements Filter {

    private final boolean contractValidated;

    @Override
    public Response filter(FilterableRequestSpecification request, FilterableResponseSpecification spec,
                           FilterContext context) {
        Response response = context.next(request, spec);
        ApiCoverage.record(request.getMethod(), request.getUserDefinedPath(), response.statusCode(), contractValidated);
        return response;
    }
}

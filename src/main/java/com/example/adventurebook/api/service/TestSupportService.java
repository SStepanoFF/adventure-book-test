package com.example.adventurebook.api.service;

import com.example.adventurebook.api.ApiClient;
import io.qameta.allure.Step;
import io.restassured.response.Response;
import lombok.RequiredArgsConstructor;

/** Test-only support endpoint (/api/test/*). */
@RequiredArgsConstructor
public class TestSupportService {

    public static final String RESET = "/api/test/reset";

    private final ApiClient client;

    public TestSupportService() {
        this(ApiClient.standard());
    }

    @Step("Reset test state / database")
    public Response reset() {
        return client.request().post(RESET);
    }
}
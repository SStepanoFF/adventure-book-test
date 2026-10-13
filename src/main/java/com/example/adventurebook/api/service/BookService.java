package com.example.adventurebook.api.service;

import com.example.adventurebook.api.ApiClient;
import com.example.adventurebook.api.dto.request.BookRequestDto;
import com.example.adventurebook.api.dto.response.BookResponseDto;
import io.qameta.allure.Step;
import io.restassured.response.Response;
import lombok.RequiredArgsConstructor;
import io.restassured.specification.RequestSpecification;
import java.util.Arrays;
import java.util.List;

/**
 * Catalog endpoints (/api/books); the group's paths live here, next to the methods that use them.
 * Convention: a method WITHOUT suffix returns the plain REST Assured {@link Response} (any status, for negative
 * tests); a method with the {@code Ok} suffix asserts the success status and returns the parsed DTO (preconditions).
 */
@RequiredArgsConstructor
public class BookService {

    public static final String BOOKS = "/api/books";
    public static final String BOOK = "/api/books/{bookId}";

    private final ApiClient client;

    public BookService() {
        this(ApiClient.standard());
    }

    public Response list(String search, String tag) {
        RequestSpecification request = client.request();
        if (search != null) {
            request.queryParam("search", search);
        }
        if (tag != null) {
            request.queryParam("tag", tag);
        }
        return request.get(BOOKS);
    }

    public Response list() {
        return list(null, null);
    }

    @Step("Get Book by Id='{bookId}'")
    public Response get(Object bookId) {
        return client.request().pathParam("bookId", bookId).get(BOOK);
    }

    public Response add(BookRequestDto book) {
        return client.request().body(book).post(BOOKS);
    }

    /** Raw JSON body, for malformed-payload tests. */
    public Response addRaw(String json) {
        return client.request().body(json).post(BOOKS);
    }

    // ---------- success variants: assert the status, return the DTO ----------

    public List<BookResponseDto> listOk(String search, String tag) {
        return Arrays.asList(list(search, tag).then().statusCode(200).extract().as(BookResponseDto[].class));
    }

    public List<BookResponseDto> listOk() {
        return listOk(null, null);
    }

    public BookResponseDto getOk(Object bookId) {
        return get(bookId).then().statusCode(200).extract().as(BookResponseDto.class);
    }

    public BookResponseDto addOk(BookRequestDto book) {
        return add(book).then().statusCode(201).extract().as(BookResponseDto.class);
    }
}

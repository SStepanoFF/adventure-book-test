package com.example.adventurebook.api.service;

import com.example.adventurebook.api.ApiClient;
import com.example.adventurebook.api.dto.request.StartGameRequestDto;
import com.example.adventurebook.api.dto.response.GameStateResponseDto;
import io.qameta.allure.Step;
import io.restassured.response.Response;
import lombok.RequiredArgsConstructor;

/** Game endpoints (/api/games); the group's paths live here. Same convention as {@link BookService}. */
@RequiredArgsConstructor
public class GameService {

    public static final String GAMES = "/api/games";
    public static final String GAME = "/api/games/{sessionId}";
    public static final String CHOICE = "/api/games/{sessionId}/choices/{optionIndex}";
    public static final String SAVE = "/api/games/{sessionId}/save";
    public static final String RESTART = "/api/games/{sessionId}/restart";

    private final ApiClient client;

    public GameService() {
        this(ApiClient.standard());
    }

    @Step("Start game session for book ID={bookId}")
    public Response start(Long bookId) {
        return client.request().body(new StartGameRequestDto(bookId)).post(GAMES);
    }

    /** Raw JSON body, for malformed-payload tests. */
    @Step("Start game session with raw payload")
    public Response startRaw(String json) {
        return client.request().body(json).post(GAMES);
    }

    @Step("Get game state for session '{sessionId}'")
    public Response get(String sessionId) {
        return client.request().pathParam("sessionId", sessionId).get(GAME);
    }

    @Step("Make choice optionIndex={optionIndex} for session '{sessionId}'")
    public Response choose(String sessionId, int optionIndex) {
        return client.request().pathParam("sessionId", sessionId).pathParam("optionIndex", optionIndex).post(CHOICE);
    }

    @Step("Save game session '{sessionId}'")
    public Response save(String sessionId) {
        return client.request().pathParam("sessionId", sessionId).post(SAVE);
    }

    @Step("Restart game session '{sessionId}'")
    public Response restart(String sessionId) {
        return client.request().pathParam("sessionId", sessionId).post(RESTART);
    }

    // ---------- success variants: assert the status, return the DTO ----------

    @Step("Start game session for book ID={bookId} (expecting 201 Created)")
    public GameStateResponseDto startOk(long bookId) {
        return start(bookId).then().statusCode(201).extract().as(GameStateResponseDto.class);
    }

    @Step("Get game state for session '{sessionId}' (expecting 200 OK)")
    public GameStateResponseDto getOk(String sessionId) {
        return get(sessionId).then().statusCode(200).extract().as(GameStateResponseDto.class);
    }

    @Step("Make choice optionIndex={optionIndex} for session '{sessionId}' (expecting 200 OK)")
    public GameStateResponseDto chooseOk(String sessionId, int optionIndex) {
        return choose(sessionId, optionIndex).then().statusCode(200).extract().as(GameStateResponseDto.class);
    }

    @Step("Save game session '{sessionId}' (expecting 200 OK)")
    public GameStateResponseDto saveOk(String sessionId) {
        return save(sessionId).then().statusCode(200).extract().as(GameStateResponseDto.class);
    }

    @Step("Restart game session '{sessionId}' (expecting 200 OK)")
    public GameStateResponseDto restartOk(String sessionId) {
        return restart(sessionId).then().statusCode(200).extract().as(GameStateResponseDto.class);
    }

    /** Plays a sequence of option indexes and returns the last state. */
    @Step("Play sequence of options {optionIndexes} for session '{sessionId}'")
    public GameStateResponseDto playOk(String sessionId, int... optionIndexes) {
        GameStateResponseDto state = getOk(sessionId);
        for (int index : optionIndexes) {
            state = chooseOk(sessionId, index);
        }
        return state;
    }
}
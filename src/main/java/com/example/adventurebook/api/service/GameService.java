package com.example.adventurebook.api.service;

import com.example.adventurebook.api.ApiClient;
import com.example.adventurebook.api.dto.request.StartGameRequestDto;
import com.example.adventurebook.api.dto.response.GameStateResponseDto;
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

    public Response start(Long bookId) {
        return client.request().body(new StartGameRequestDto(bookId)).post(GAMES);
    }

    /** Raw JSON body, for malformed-payload tests. */
    public Response startRaw(String json) {
        return client.request().body(json).post(GAMES);
    }

    public Response get(String sessionId) {
        return client.request().pathParam("sessionId", sessionId).get(GAME);
    }

    public Response choose(String sessionId, int optionIndex) {
        return client.request().pathParam("sessionId", sessionId).pathParam("optionIndex", optionIndex).post(CHOICE);
    }

    public Response save(String sessionId) {
        return client.request().pathParam("sessionId", sessionId).post(SAVE);
    }

    public Response restart(String sessionId) {
        return client.request().pathParam("sessionId", sessionId).post(RESTART);
    }

    // ---------- success variants: assert the status, return the DTO ----------

    public GameStateResponseDto startOk(long bookId) {
        return start(bookId).then().statusCode(201).extract().as(GameStateResponseDto.class);
    }

    public GameStateResponseDto getOk(String sessionId) {
        return get(sessionId).then().statusCode(200).extract().as(GameStateResponseDto.class);
    }

    public GameStateResponseDto chooseOk(String sessionId, int optionIndex) {
        return choose(sessionId, optionIndex).then().statusCode(200).extract().as(GameStateResponseDto.class);
    }

    public GameStateResponseDto saveOk(String sessionId) {
        return save(sessionId).then().statusCode(200).extract().as(GameStateResponseDto.class);
    }

    public GameStateResponseDto restartOk(String sessionId) {
        return restart(sessionId).then().statusCode(200).extract().as(GameStateResponseDto.class);
    }

    /** Plays a sequence of option indexes and returns the last state. */
    public GameStateResponseDto playOk(String sessionId, int... optionIndexes) {
        GameStateResponseDto state = getOk(sessionId);
        for (int index : optionIndexes) {
            state = chooseOk(sessionId, index);
        }
        return state;
    }
}

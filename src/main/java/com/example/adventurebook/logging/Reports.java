package com.example.adventurebook.logging;

import io.qameta.allure.Allure;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The ONLY place that talks to the Allure runtime API for attachments, so a change of reporter
 * (or of the Allure API) touches a single class.
 */
public final class Reports {

    private Reports() {
    }

    public static void text(String name, String content) {
        Allure.attachment(name, content);
    }

    public static void binary(String name, byte[] content) {
        Allure.attachment(name, new ByteArrayInputStream(content));
    }

    public static void file(String name, Path path) {
        try (InputStream in = Files.newInputStream(path)) {
            Allure.attachment(name, in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}

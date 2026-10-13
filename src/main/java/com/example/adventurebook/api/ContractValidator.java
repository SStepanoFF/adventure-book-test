package com.example.adventurebook.api;

import com.atlassian.oai.validator.restassured.OpenApiValidationFilter;
import com.example.adventurebook.config.TafConfig;
import io.restassured.filter.Filter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Contract testing of REAL traffic: every request/response that goes through the filter is validated against
 * the OpenAPI document. (OASDiff complements this: it compares two versions of the spec - see scripts/oasdiff.sh.)
 */
public final class ContractValidator {

    private final OpenApiValidationFilter filter;

    private ContractValidator(String specUrlOrContent) {
        this.filter = new OpenApiValidationFilter(specUrlOrContent);
    }

    /** Spec location comes from config key {@code openapi.spec}: http(s) URL, classpath resource or file path. */
    public static ContractValidator fromConfig() {
        String ref = TafConfig.get().openApiSpec();
        if (ref.startsWith("http://") || ref.startsWith("https://")) {
            return new ContractValidator(ref);
        }
        return new ContractValidator(readLocal(ref));
    }

    public Filter filter() {
        return filter;
    }

    private static String readLocal(String ref) {
        try (InputStream in = ContractValidator.class.getClassLoader().getResourceAsStream(ref)) {
            if (in != null) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            return Files.readString(Path.of(ref));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read OpenAPI spec '" + ref + "'", e);
        }
    }
}

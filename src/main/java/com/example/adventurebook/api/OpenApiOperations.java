package com.example.adventurebook.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;

/** Lists the operations ("METHOD /path") described by an OpenAPI document (classpath resource, file or URL). */
public final class OpenApiOperations {

    private static final Set<String> HTTP_METHODS = Set.of("get", "post", "put", "delete", "patch", "head", "options");

    private OpenApiOperations() {
    }

    public static Set<String> load(String ref) {
        JsonNode root;
        try {
            root = new ObjectMapper(new YAMLFactory()).readTree(read(ref));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot parse OpenAPI document '" + ref + "'", e);
        }
        Set<String> operations = new TreeSet<>();
        root.path("paths").fields().forEachRemaining(path ->
                path.getValue().fieldNames().forEachRemaining(method -> {
                    if (HTTP_METHODS.contains(method.toLowerCase())) {
                        operations.add(ApiCoverage.key(method, path.getKey()));
                    }
                }));
        return operations;
    }

    private static String read(String ref) throws IOException {
        if (ref.startsWith("http://") || ref.startsWith("https://")) {
            try (InputStream in = URI.create(ref).toURL().openStream()) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
        try (InputStream in = OpenApiOperations.class.getClassLoader().getResourceAsStream(ref)) {
            if (in != null) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
        return Files.readString(Path.of(ref));
    }
}

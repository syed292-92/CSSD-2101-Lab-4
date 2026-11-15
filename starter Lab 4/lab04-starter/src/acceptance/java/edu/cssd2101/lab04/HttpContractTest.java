package edu.cssd2101.lab04;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HttpContractTest {
    static final Path DIR = createDirectory();

    static Path createDirectory() {
        try {
            return Files.createTempDirectory("lab04-http-");
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("catalogue.file", () -> DIR.resolve("books.json").toString());
    }

    @LocalServerPort int port;
    final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    final JsonMapper mapper = JsonMapper.builder().build();

    HttpResponse<String> call(String method, String route, String json) throws Exception {
        var builder =
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + route))
                        .timeout(Duration.ofSeconds(5));
        if (json != null) builder.header("Content-Type", "application/json");
        return client.send(
                builder.method(
                                method,
                                json == null
                                        ? HttpRequest.BodyPublishers.noBody()
                                        : HttpRequest.BodyPublishers.ofString(json))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void realHttpCrudStatusHeadersAndNegativeRequests() throws Exception {
        String body = "{\"title\":\"HTTP Java\",\"priceCents\":1299,\"format\":\"PRINT\"}";
        var created = call("POST", "/api/books", body);
        assertEquals(201, created.statusCode());
        assertTrue(
                created.headers()
                        .firstValue("content-type")
                        .orElse("")
                        .contains("application/json"));
        String route = created.headers().firstValue("Location").orElseThrow();
        String id = mapper.readTree(created.body()).get("id").asString();
        assertEquals("/api/books/" + id, route);
        assertEquals(200, call("GET", route, null).statusCode());
        assertEquals(200, call("GET", "/api/books", null).statusCode());
        for (String invalid :
                new String[] {
                    "{",
                    "{}",
                    "null",
                    body.replace("1299", "-1"),
                    body.replace("1299", "1.5"),
                    body.replace("PRINT", "UNKNOWN"),
                    body.replace("HTTP Java", " "),
                    body.replace("{", "{\"id\":\"spoofed\",")
                }) {
            var rejected = call("PUT", route, invalid);
            assertEquals(400, rejected.statusCode(), invalid + rejected.body());
            assertTrue(rejected.body().contains("INVALID_REQUEST"));
            assertEquals(
                    "HTTP Java",
                    mapper.readTree(call("GET", route, null).body()).get("title").asString());
        }
        var replaced = call("PUT", route, body.replace("HTTP Java", "Revised"));
        assertEquals(200, replaced.statusCode());
        assertEquals(id, mapper.readTree(replaced.body()).get("id").asString());
        assertEquals(400, call("GET", "/api/books/bad", null).statusCode());
        var removed = call("DELETE", route, null);
        assertEquals(204, removed.statusCode());
        assertEquals("", removed.body());
        assertEquals(404, call("GET", route, null).statusCode());
        assertEquals(404, call("DELETE", route, null).statusCode());
        assertEquals(404, call("PUT", route, body).statusCode());
        var malformed = call("POST", "/api/books", "{");
        assertFalse(malformed.body().contains("exception"));
        assertFalse(malformed.body().contains(DIR.toString()));
    }

    @org.junit.jupiter.api.AfterEach
    void closeClient() {
        client.close();
    }

    @AfterAll
    static void clean() throws Exception {
        try (var files = Files.walk(DIR)) {
            for (Path file : files.sorted(java.util.Comparator.reverseOrder()).toList())
                Files.delete(file);
        }
    }
}

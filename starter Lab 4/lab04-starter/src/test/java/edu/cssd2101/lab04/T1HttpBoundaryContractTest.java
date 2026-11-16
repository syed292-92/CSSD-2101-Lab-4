package edu.cssd2101.lab04;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Full-stack HTTP boundary tests verifying raw JSON token rules, error
 * envelopes, and state preservation across invalid requests for Task T1.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class T1HttpBoundaryContractTest {

    @LocalServerPort
    private int port;

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + port + path);
    }

    private HttpResponse<String> sendJson(String method, String path, String rawJson) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/json")
                .method(method, HttpRequest.BodyPublishers.ofString(rawJson))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri(path)).GET().build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private void assertSafeTwoFieldErrorEnvelope(HttpResponse<String> response) throws Exception {
        assertEquals(400, response.statusCode());
        JsonNode root = mapper.readTree(response.body());
        assertTrue(root.isObject(), "Error response must be a JSON object");

        Set<String> fieldNames = new TreeSet<>(root.propertyNames());
        assertEquals(Set.of("code", "message"), fieldNames, "Error envelope must only contain code and message");

        String body = response.body();
        assertFalse(body.contains("edu.cssd2101"), "Must not leak internal package names");
        assertFalse(body.contains("Exception"), "Must not leak Java exception class names");
        assertFalse(body.contains("jackson"), "Must not leak serializer internals");
        assertFalse(body.contains("\n\tat "), "Must not leak stack traces");
    }

    @Test
    void postAcceptsZeroAndMaximumPricesAndReturnsConsistentLocation() throws Exception {
        HttpResponse<String> freeRes = sendJson(
                "POST",
                "/api/books",
                "{\"title\":\"Free Book\",\"priceCents\":0,\"format\":\"EBOOK\"}");
        assertEquals(201, freeRes.statusCode());
        String location = freeRes.headers().firstValue("Location").orElseThrow();
        JsonNode created = mapper.readTree(freeRes.body());
        assertTrue(location.endsWith("/api/books/" + created.get("id").asText()));
        assertEquals(0L, created.get("priceCents").asLong());

        HttpResponse<String> maxRes = sendJson(
                "POST",
                "/api/books",
                "{\"title\":\"Max Book\",\"priceCents\":100000000,\"format\":\"PRINT\"}");
        assertEquals(201, maxRes.statusCode());
        assertEquals(100_000_000L, mapper.readTree(maxRes.body()).get("priceCents").asLong());
    }

    @Test
    void postRejectsAllInvalidBoundaryTokensWithSafeErrorEnvelope() throws Exception {
        List<String> invalidPayloads = List.of(
                // Missing priceCents
                "{\"title\":\"No Price\",\"format\":\"PRINT\"}",
                // Fractional cents token
                "{\"title\":\"Fractional\",\"priceCents\":12.99,\"format\":\"PRINT\"}",
                // Unknown format token
                "{\"title\":\"Audio\",\"priceCents\":1299,\"format\":\"AUDIOBOOK\"}",
                // Blank whitespace-only title
                "{\"title\":\"    \",\"priceCents\":1299,\"format\":\"PRINT\"}",
                // Attempted id field in request body
                "{\"id\":\"11111111-1111-1111-1111-111111111111\",\"title\":\"Injected ID\",\"priceCents\":1299,\"format\":\"PRINT\"}");

        for (String payload : invalidPayloads) {
            HttpResponse<String> res = sendJson("POST", "/api/books", payload);
            assertSafeTwoFieldErrorEnvelope(res);
        }

        HttpResponse<String> listRes = get("/api/books");
        assertEquals(200, listRes.statusCode());
        assertEquals(0, mapper.readTree(listRes.body()).size(), "Catalogue must remain empty after rejected POSTs");
    }

    @Test
    void rejectedPutRequestsLeaveStoredBookUnchangedOverHttp() throws Exception {
        HttpResponse<String> createRes = sendJson(
                "POST",
                "/api/books",
                "{\"title\":\"Clean Architecture\",\"priceCents\":3499,\"format\":\"PRINT\"}");
        assertEquals(201, createRes.statusCode());
        String location = createRes.headers().firstValue("Location").orElseThrow();
        JsonNode original = mapper.readTree(createRes.body());

        List<String> invalidUpdates = List.of(
                "{\"title\":\"Hacked\",\"priceCents\":19.99,\"format\":\"PRINT\"}",
                "{\"title\":\"   \",\"priceCents\":1999,\"format\":\"PRINT\"}",
                "{\"title\":\"Hacked\",\"format\":\"PRINT\"}",
                "{\"title\":\"Hacked\",\"priceCents\":1999,\"format\":\"HARDCOVER\"}",
                "{\"id\":\"" + original.get("id").asText()
                        + "\",\"title\":\"Hacked\",\"priceCents\":1999,\"format\":\"PRINT\"}");

        for (String badBody : invalidUpdates) {
            HttpResponse<String> putRes = sendJson("PUT", location, badBody);
            assertSafeTwoFieldErrorEnvelope(putRes);

            HttpResponse<String> getRes = get(location);
            assertEquals(200, getRes.statusCode());
            JsonNode current = mapper.readTree(getRes.body());
            assertEquals(original.get("id"), current.get("id"));
            assertEquals("Clean Architecture", current.get("title").asText());
            assertEquals(3499L, current.get("priceCents").asLong());
            assertEquals("PRINT", current.get("format").asText());
        }
    }
}
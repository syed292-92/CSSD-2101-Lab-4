package edu.cssd2101.lab04;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {Main.class, HttpStorageFailureTest.FailingConfiguration.class})
class HttpStorageFailureTest {
    static final Path DIRECTORY = createDirectory();

    static Path createDirectory() {
        try {
            return Files.createTempDirectory("lab04-http-failure-");
        } catch (Exception failure) {
            throw new IllegalStateException(failure);
        }
    }

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("catalogue.file", () -> DIRECTORY.resolve("books.json").toString());
    }

    @TestConfiguration
    static class FailingConfiguration {
        @Bean
        @Primary
        CatalogueService rejectingService() {
            return new CatalogueService(
                    new CataloguePort() {
                        public List<Book> load() {
                            return List.of();
                        }

                        public void save(List<Book> books) {
                            throw new StorageException(
                                    new java.io.IOException("/private/internal-secret"));
                        }
                    });
        }
    }

    @LocalServerPort int port;

    @Test
    void persistenceFailureIs503AndDoesNotLeakOrPublish() throws Exception {
        try (var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()) {
            var uri = URI.create("http://127.0.0.1:" + port + "/api/books");
            var post =
                    HttpRequest.newBuilder(uri)
                            .timeout(Duration.ofSeconds(5))
                            .header("Content-Type", "application/json")
                            .POST(
                                    HttpRequest.BodyPublishers.ofString(
                                            "{\"title\":\"A\",\"priceCents\":1,\"format\":\"PRINT\"}"))
                            .build();
            var failure = client.send(post, HttpResponse.BodyHandlers.ofString());
            assertEquals(503, failure.statusCode());
            assertTrue(failure.body().contains("STORAGE_UNAVAILABLE"));
            assertFalse(failure.body().contains("internal-secret"));
            assertFalse(failure.body().contains("IOException"));
            var get =
                    client.send(
                            HttpRequest.newBuilder(uri)
                                    .timeout(Duration.ofSeconds(5))
                                    .GET()
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());
            assertEquals(200, get.statusCode());
            assertEquals("[]", get.body());
        }
    }

    @AfterAll
    static void clean() throws Exception {
        Files.delete(DIRECTORY);
    }
}

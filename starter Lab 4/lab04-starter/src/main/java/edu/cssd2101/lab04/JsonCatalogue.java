package edu.cssd2101.lab04;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * JSON adapter for one application process on a filesystem supporting atomic rename. Atomic rename
 * gives replacement atomicity, not guaranteed power-loss durability.
 */
public final class JsonCatalogue implements CataloguePort {
    private final Path file;
    private final JsonMapper mapper =
            JsonMapper.builder()
                    .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
                    .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                    .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
                    .build();

    /**
     * Creates an adapter without touching storage.
     *
     * @param file destination owned exclusively by this application instance
     */
    public JsonCatalogue(Path file) {
        this.file = Objects.requireNonNull(file).toAbsolutePath();
    }

    @Override
    public List<Book> load() {
        try {
            if (Files.notExists(file)) return List.of();
            return List.copyOf(
                    Arrays.asList(mapper.readValue(Files.readAllBytes(file), Book[].class)));
        } catch (Exception failure) {
            throw new StorageException(failure);
        }
    }

    @Override
    public void save(List<Book> books) {
        // TODO T4 atomic snapshot persistence: follow the task contract in the handout.
        throw new UnsupportedOperationException("T4 atomic snapshot persistence not implemented");
    }
}

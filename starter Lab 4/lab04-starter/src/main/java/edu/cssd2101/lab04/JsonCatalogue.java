package edu.cssd2101.lab04;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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
        // Use a temporary file to avoid leaving a corrupted file if the process is interrupted
        Path temporary = null;
        try {
            // Convert the list of books to JSON bytes
            byte[] json = mapper.writeValueAsBytes(books);
            // Get the parent directory of the target file and create it if it doesn't exist
            Files.createDirectories(file.getParent());
            // Create a temporary file in the same directory as the target file
            temporary = Files.createTempFile(file.getParent(), ".catalogue-", ".tmp");
            // Write the JSON data to the temporary file
            Files.write(temporary, json);
            // Move the temporary file to the target file atomically, replacing any existing file
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        }
        // Catch any exceptions that occur during the save process
        catch (Exception failure) {
            // Report failure 
            throw new StorageException(failure);
        }
        finally {
            // Attempt to delete the temporary file if it still exists
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (java.io.IOException ignored) {
                    // Ignore failure to delete temporary file
                }
            }
        }
    }
}

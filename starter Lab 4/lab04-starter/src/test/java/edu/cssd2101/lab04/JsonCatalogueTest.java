package edu.cssd2101.lab04;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class JsonCatalogueTest {
    // Temporary directory provided by JUnit for file operations
    @TempDir
    Path temporary;
    // Test that a missing file results in an empty catalogue without creating the file
    @Test
    void missingFileLoadsEmptyCatalogue() {
        Path file = temporary.resolve("catalogue.json");
        CatalogueService service = new CatalogueService(new JsonCatalogue(file));
        assertTrue(service.all().isEmpty());
        assertFalse(Files.exists(file));
    }
    // Test that a JSON file is loaded correctly
    @Test
    void unicodeBookIsSavedAndLoaded() {
        Path file = temporary.resolve("data/catalogue.json");
        CatalogueService service = new CatalogueService(new JsonCatalogue(file));
        Book book = service.create(new BookRequest("Java café", 1299L, Format.PRINT));
        CatalogueService restarted = new CatalogueService(new JsonCatalogue(file));
        assertEquals(List.of(book), restarted.all());
    }
    // Test that replacing a book is saved and loaded correctly
    @Test
    void replacementIsSavedAndLoaded() {
        Path file = temporary.resolve("catalogue.json");
        CatalogueService service = new CatalogueService(new JsonCatalogue(file));
        Book first = service.create(new BookRequest("First book", 1299L, Format.PRINT));
        Book second = service.create(new BookRequest("Second book", 2000L, Format.PRINT));
        Book updated = service.replace(first.id(), new BookRequest("Updated book", 1500L, Format.EBOOK));
        CatalogueService restarted = new CatalogueService(new JsonCatalogue(file));
        assertEquals(first.id(), updated.id());
        assertEquals("Updated book", updated.title());
        assertEquals(1500L, updated.priceCents());
        assertEquals(Format.EBOOK, updated.format());
        assertEquals(List.of(updated, second), restarted.all());
    }
    // Test that deleting a book is saved and loaded correctly
    @Test
    void deletionIsSavedAndLoaded() {
        Path file = temporary.resolve("catalogue.json");
        CatalogueService service = new CatalogueService(new JsonCatalogue(file));
        Book first = service.create(new BookRequest("First book", 1299L, Format.PRINT));
        Book second = service.create(new BookRequest("Second book", 2000L, Format.PRINT));
        service.delete(first.id());
        CatalogueService restarted = new CatalogueService(new JsonCatalogue(file));
        assertEquals(List.of(second), restarted.all());
        restarted.delete(second.id());
        CatalogueService restartedAgain = new CatalogueService(new JsonCatalogue(file));
        assertTrue(restartedAgain.all().isEmpty());
    }
    // Test that a corrupt JSON file fails to load without changing its contents
    @Test
    void corruptFileFailsWithoutChangingContents() throws IOException {
        Path file = temporary.resolve("catalogue.json");
        Files.writeString(file, "this is not valid JSON");
        byte[] original = Files.readAllBytes(file);
        assertThrows(StorageException.class, () -> new CatalogueService(new JsonCatalogue(file)));
        assertArrayEquals(original, Files.readAllBytes(file));
    }
    // Test that duplicate stored IDs fail at initialization without changing the file
    @Test
    void duplicateStoredIdsFailAtStartup() throws IOException {
        Path file = temporary.resolve("catalogue.json");
        JsonCatalogue adapter = new JsonCatalogue(file);
        Book book = new Book("00000000-0000-0000-0000-000000000001", "Java", 1299L, Format.PRINT);
        adapter.save(List.of(book, book));
        byte[] original = Files.readAllBytes(file);
        assertThrows(StorageException.class, () -> new CatalogueService(new JsonCatalogue(file)));
        assertArrayEquals(original, Files.readAllBytes(file));
    }
    // Test that attempting to save to a path that is a directory fails without changing the directory contents
    @Test
    void fileUsedAsDirectoryCausesWriteFailure() throws IOException {
        Path blocker = temporary.resolve("not-a-directory");
        Files.writeString(blocker, "keep this content");
        JsonCatalogue adapter = new JsonCatalogue(blocker.resolve("catalogue.json"));
        assertThrows(StorageException.class, () -> adapter.save(List.of()));
        assertEquals("keep this content", Files.readString(blocker));
    }
    // Test that a successful save leaves no temporary files behind
    @Test
    void successfulSaveLeavesNoTemporaryFiles() throws IOException {
        Path file = temporary.resolve("catalogue.json");
        CatalogueService service = new CatalogueService(new JsonCatalogue(file));
        service.create(new BookRequest("Java", 1299L, Format.PRINT));
        // The folder should contain only catalogue.json.
        try (var files = Files.list(temporary)) {
            assertEquals(List.of(file), files.toList());
        }
    }
    // Test that a failed move during save removes the temporary file and leaves the original directory intact
    @Test
    void failedMoveRemovesTemporaryFile() throws IOException {
        // A nonempty directory cannot be replaced by the JSON file.
        Path destination = Files.createDirectory(temporary.resolve("catalogue.json"));
        Path existingFile = destination.resolve("keep.txt");
        Files.writeString(existingFile, "unchanged");
        JsonCatalogue adapter = new JsonCatalogue(destination);
        assertThrows(StorageException.class, () -> adapter.save(List.of()));
        assertEquals("unchanged", Files.readString(existingFile));
        // Only the original directory should remain.
        try (var files = Files.list(temporary)) {
            assertEquals(List.of(destination), files.toList());
        }
    }
    // Test that the main method uses the configured JSON file for persistence
    @Test
    void mainUsesConfiguredJsonFile() {
        Path file = temporary.resolve("catalogue.json");
        Main main = new Main();
        CatalogueService service = main.catalogueService(file.toString());
        Book book = service.create(new BookRequest("Java", 1299L, Format.PRINT));
        CatalogueService restarted = main.catalogueService(file.toString());
        assertEquals(List.of(book), restarted.all());
    }
    // A simple catalogue adapter that can be configured to fail on save operations
    static class FailingCatalogue implements CataloguePort {
        private List<Book> books = List.of();
        private boolean fail;
        
        @Override
        // Loads the current list of books
        public List<Book> load() {
            return books;
        }
        // Saves the provided list of books, but might throw a StorageException
        @Override
        public void save(List<Book> next) {
            if (fail) {
                throw new StorageException(new IOException("Test save failure"));
            }
            books = List.copyOf(next);
        }
    }
    // Test that a failed create operation preserves both in-memory and on-disk state
    @Test
    void failedCreatePreservesMemoryAndStorage() {
        FailingCatalogue adapter = new FailingCatalogue();
        CatalogueService service = new CatalogueService(adapter);
        Book original = service.create(new BookRequest("Original", 1299L, Format.PRINT));
        adapter.fail = true;
        assertThrows(StorageException.class, () -> service.create(new BookRequest("New book", 2000L, Format.EBOOK)));
        assertEquals(List.of(original), service.all());
        assertEquals(List.of(original), adapter.load());
    }
    // Test that a failed replace operation preserves both in-memory and on-disk state
    @Test
    void failedReplacementPreservesMemoryAndStorage() {
        FailingCatalogue adapter = new FailingCatalogue();
        CatalogueService service = new CatalogueService(adapter);
        Book original = service.create(new BookRequest("Original", 1299L, Format.PRINT));
        adapter.fail = true;
        assertThrows(StorageException.class, () -> service.replace(original.id(), new BookRequest("Changed", 2000L, Format.EBOOK)));
        assertEquals(List.of(original), service.all());
        assertEquals(List.of(original), adapter.load());
    }
    // Test that a failed delete operation preserves both in-memory and on-disk state
    @Test
    void failedDeletionPreservesMemoryAndStorage() {
        FailingCatalogue adapter = new FailingCatalogue();
        CatalogueService service = new CatalogueService(adapter);
        Book original = service.create(new BookRequest("Original", 1299L, Format.PRINT));
        adapter.fail = true;
        assertThrows(StorageException.class, () -> service.delete(original.id()));
        assertEquals(List.of(original), service.all());
        assertEquals(List.of(original), adapter.load());
    }
}
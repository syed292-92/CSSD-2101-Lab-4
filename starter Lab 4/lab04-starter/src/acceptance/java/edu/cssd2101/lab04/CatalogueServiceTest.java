package edu.cssd2101.lab04;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

class CatalogueServiceTest {
    @TempDir Path temporary;

    static class Memory implements CataloguePort {
        List<Book> books = List.of();
        boolean fail;

        public List<Book> load() {
            return books;
        }

        public void save(List<Book> next) {
            if (fail) throw new StorageException(new java.io.IOException("private/path"));
            books = List.copyOf(next);
        }
    }

    BookRequest request(String title) {
        return new BookRequest(title, 1299L, Format.PRINT);
    }

    @Test
    void crudSnapshotsIdentityAndMissing() {
        var memory = new Memory();
        var service = new CatalogueService(memory);
        assertTrue(service.all().isEmpty());
        var a = service.create(request(" A "));
        var b = service.create(request("B"));
        assertEquals("A", a.title());
        assertNotEquals(a.id(), b.id());
        assertEquals(a, service.find(a.id()));
        var old = service.all();
        var updated = service.replace(a.id(), new BookRequest("Revised", 0L, Format.EBOOK));
        assertEquals(a.id(), updated.id());
        assertEquals(List.of(updated, b), service.all());
        assertEquals(List.of(a, b), old);
        assertThrows(UnsupportedOperationException.class, () -> old.clear());
        service.delete(a.id());
        assertEquals(List.of(b), service.all());
        assertEquals(List.of(a, b), old);
        assertThrows(MissingBookException.class, () -> service.find(a.id()));
        assertThrows(MissingBookException.class, () -> service.delete(a.id()));
        assertThrows(MissingBookException.class, () -> service.replace(a.id(), request("Missing")));
    }

    @Test
    void invalidRequestsNeverMutateState() {
        var service = new CatalogueService(new Memory());
        var a = service.create(request("A"));
        for (BookRequest bad :
                List.of(
                        new BookRequest(null, 1L, Format.PRINT),
                        new BookRequest(" ", 1L, Format.PRINT),
                        new BookRequest("x".repeat(121), 1L, Format.PRINT),
                        new BookRequest("A", null, Format.PRINT),
                        new BookRequest("A", -1L, Format.PRINT),
                        new BookRequest("A", 100000001L, Format.PRINT),
                        new BookRequest("A", 1L, null))) {
            assertThrows(IllegalArgumentException.class, () -> service.create(bad));
            assertThrows(IllegalArgumentException.class, () -> service.replace(a.id(), bad));
            assertEquals(List.of(a), service.all());
        }
        assertThrows(IllegalArgumentException.class, () -> service.create(null));
        assertThrows(IllegalArgumentException.class, () -> service.replace(a.id(), null));
        assertThrows(IllegalArgumentException.class, () -> service.find("bad"));
        assertThrows(IllegalArgumentException.class, () -> service.find(null));
        assertThrows(IllegalArgumentException.class, () -> service.delete("bad"));
        assertThrows(IllegalArgumentException.class, () -> service.delete(null));
        assertEquals(
                100000000L,
                service.create(new BookRequest("x".repeat(120), 100000000L, Format.EBOOK))
                        .priceCents());
    }

    @Test
    void everyFailedWritePreservesMemoryAndStoredSnapshot() {
        var memory = new Memory();
        var service = new CatalogueService(memory);
        var a = service.create(request("A"));
        memory.fail = true;
        assertThrows(StorageException.class, () -> service.create(request("B")));
        assertThrows(StorageException.class, () -> service.replace(a.id(), request("Changed")));
        assertThrows(StorageException.class, () -> service.delete(a.id()));
        assertEquals(List.of(a), service.all());
        assertEquals(List.of(a), memory.books);
        memory.fail = false;
        service.delete(a.id());
        assertTrue(service.all().isEmpty());
    }

    @Test
    void jsonRoundTripRestartAndDeletionPersistence() throws Exception {
        Path file = temporary.resolve("nested/catalogue.json");
        var first = new CatalogueService(new JsonCatalogue(file));
        var a = first.create(request("Java café and Unicode"));
        var b = first.create(request("B"));
        var restarted = new CatalogueService(new JsonCatalogue(file));
        assertEquals(first.all(), restarted.all());
        restarted.replace(a.id(), request("Updated"));
        restarted.delete(b.id());
        assertEquals(
                List.of(restarted.find(a.id())),
                new CatalogueService(new JsonCatalogue(file)).all());
        try (var files = Files.list(file.getParent())) {
            assertEquals(1, files.count());
        }
    }

    @Test
    void corruptFilesAndDuplicateIdsFailClosed() throws Exception {
        Path file = temporary.resolve("catalogue.json");
        for (String invalid :
                List.of(
                        "{oops",
                        "null",
                        "[null]",
                        "[{\"id\":\"bad\",\"title\":\"A\",\"priceCents\":1,\"format\":\"PRINT\"}]")) {
            Files.writeString(file, invalid);
            assertThrows(
                    StorageException.class, () -> new CatalogueService(new JsonCatalogue(file)));
            assertEquals(invalid, Files.readString(file));
        }
        var memory = new Memory();
        var a = new CatalogueService(memory).create(request("A"));
        memory.books = List.of(a, a);
        assertThrows(StorageException.class, () -> new CatalogueService(memory));
    }

    @Test
    void realFilesystemWriteFailureLeavesPreviousState() throws Exception {
        Path blocker = temporary.resolve("not-a-directory");
        Files.writeString(blocker, "owned fixture");
        var service = new CatalogueService(new Memory());
        var adapter = new JsonCatalogue(blocker.resolve("catalogue.json"));
        assertThrows(StorageException.class, () -> adapter.save(service.all()));
        assertEquals("owned fixture", Files.readString(blocker));
        assertThrows(StorageException.class, () -> new JsonCatalogue(temporary).load());
    }

    @Test
    void concurrentCreatesDoNotLoseUpdates() throws Exception {
        var service = new CatalogueService(new Memory());
        try (var executor = Executors.newFixedThreadPool(4)) {
            var jobs = new ArrayList<Callable<Book>>();
            for (int i = 0; i < 40; i++) {
                final int n = i;
                jobs.add(() -> service.create(request("Book " + n)));
            }
            for (var future : executor.invokeAll(jobs))
                assertNotNull(future.get(5, TimeUnit.SECONDS));
        }
        assertEquals(40, service.all().size());
        assertEquals(40, service.all().stream().map(Book::id).distinct().count());
    }

    @Test
    void persistedMissingAndNullPriceCannotBecomeFreeBooks() throws Exception {
        Path file = temporary.resolve("invalid-price.json");
        String prefix =
                "{\"id\":\"00000000-0000-0000-0000-000000000001\",\"title\":\"A\",\"format\":\"PRINT\"";
        for (String invalid :
                List.of("[" + prefix + "}]", "[" + prefix + ",\"priceCents\":null}]")) {
            Files.writeString(file, invalid);
            assertThrows(
                    StorageException.class, () -> new CatalogueService(new JsonCatalogue(file)));
            assertEquals(invalid, Files.readString(file));
        }
    }
}

package edu.cssd2101.lab04;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class BaselineTest {
    @Test
    void createReadAndValidationBaseline() {
        var service = new CatalogueService(new MemoryCatalogue());
        var book = service.create(new BookRequest("Starter Java", 1299L, Format.PRINT));
        assertEquals(book, service.find(book.id()));
        assertEquals(1, service.all().size());
        assertThrows(
                IllegalArgumentException.class,
                () -> service.create(new BookRequest(" ", 1L, Format.PRINT)));
        assertEquals(1, service.all().size());
    }
}

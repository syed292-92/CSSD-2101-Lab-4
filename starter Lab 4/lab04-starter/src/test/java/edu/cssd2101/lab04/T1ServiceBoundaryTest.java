package edu.cssd2101.lab04;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests verifying T1 domain and DTO boundary contracts directly against
 * {@link CatalogueService} without starting the HTTP layer.
 */
class T1ServiceBoundaryTest {

    private CatalogueService service;

    @BeforeEach
    void setUp() {
        this.service = new CatalogueService(new MemoryCatalogue());
    }

    @Test
    void createAcceptsZeroCentsFreeBook() {
        Book created = service.create(new BookRequest("Open Access Guide", 0L, Format.EBOOK));

        assertNotNull(created.id());
        assertEquals("Open Access Guide", created.title());
        assertEquals(0L, created.priceCents());
        assertEquals(Format.EBOOK, created.format());
        assertEquals(created, service.find(created.id()));
    }

    @Test
    void createAcceptsMaximumPermittedPrice() {
        long maxCents = 100_000_000L;
        Book created = service.create(new BookRequest("Rare First Edition", maxCents, Format.PRINT));

        assertEquals(maxCents, created.priceCents());
        assertEquals(created, service.find(created.id()));
    }

    @Test
    void createStripsLeadingAndTrailingTitleWhitespace() {
        Book created = service.create(new BookRequest("   Effective Java   ", 4599L, Format.PRINT));

        assertEquals("Effective Java", created.title());
    }

    @Test
    void createRejectsNullPriceInDtoWithoutMutatingCatalogue() {
        BookRequest missingPrice = new BookRequest("Missing Price", null, Format.PRINT);

        assertThrows(IllegalArgumentException.class, () -> service.create(missingPrice));
        assertTrue(service.all().isEmpty());
    }

    @Test
    void createRejectsOutOfRangePricesWithoutMutatingCatalogue() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.create(new BookRequest("Negative", -1L, Format.PRINT)));
        assertThrows(
                IllegalArgumentException.class,
                () -> service.create(new BookRequest("Too Expensive", 100_000_001L, Format.PRINT)));
        assertTrue(service.all().isEmpty());
    }

    @Test
    void createRejectsBlankAndOversizedTitlesWithoutMutatingCatalogue() {
        String overMaxTitle = "A".repeat(121);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.create(new BookRequest("   \t\n  ", 1000L, Format.EBOOK)));
        assertThrows(
                IllegalArgumentException.class,
                () -> service.create(new BookRequest("", 1000L, Format.EBOOK)));
        assertThrows(
                IllegalArgumentException.class,
                () -> service.create(new BookRequest(null, 1000L, Format.EBOOK)));
        assertThrows(
                IllegalArgumentException.class,
                () -> service.create(new BookRequest(overMaxTitle, 1000L, Format.EBOOK)));
        assertTrue(service.all().isEmpty());
    }

    @Test
    void rejectedReplacementPreservesOriginalImmutableSnapshot() {
        Book originalSnapshot = service.create(new BookRequest("Domain-Driven Design", 5499L, Format.PRINT));
        String id = originalSnapshot.id();

        List<BookRequest> invalidReplacements = List.of(
                new BookRequest("Updated Title", null, Format.PRINT),
                new BookRequest("Updated Title", -50L, Format.PRINT),
                new BookRequest("Updated Title", 100_000_001L, Format.PRINT),
                new BookRequest("    ", 1999L, Format.EBOOK),
                new BookRequest("Updated Title", 1999L, null));

        for (BookRequest invalid : invalidReplacements) {
            assertThrows(IllegalArgumentException.class, () -> service.replace(id, invalid));

            Book afterFailure = service.find(id);
            assertEquals(originalSnapshot, afterFailure);
            assertEquals("Domain-Driven Design", afterFailure.title());
            assertEquals(5499L, afterFailure.priceCents());
            assertEquals(Format.PRINT, afterFailure.format());
        }
    }
}
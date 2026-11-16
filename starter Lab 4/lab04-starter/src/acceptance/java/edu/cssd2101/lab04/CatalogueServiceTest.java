package edu.cssd2101.lab04;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CatalogueServiceTest {

    // holds target service instance
    private CatalogueService service;

    @Test
    void replace_test() {
        // creates a standard UUID string to serve as the target identifier for the book
        // entry
        String validID = "c9f3a2b1-d4e5-4f6a-8b7c-1a2b3c4d5e6f";

        // instantiates an inner class that implements the CataloguePort interface to
        // simulate persistence behavior.
        CataloguePort port = new CataloguePort() {
            // defines the load method required by the CatalogService constructor to
            // populate initital data
            @Override
            public List<Book> load() {
                // returns a predefined list containing one valid book record to initialize the
                // catalogue state
                return List.of(new Book(validID, "Sample Title", 1000L, Format.PRINT));
            }

            // defines an empty save method to fulfill the interface contract without
            // preforming actual disk operations
            @Override
            public void save(List<Book> books) {
            }
        };

        // initializes the target CatalogueService using the custom port containing the
        // predefined test data
        service = new CatalogueService(port);

        // constructs a new request object containing modified fields for the
        // replacement operation
        BookRequest updateRequest = new BookRequest("Updated Title", 1500L, Format.PRINT);

        // executes the replace method on the service instance and stores the returned
        // book record
        Book result = service.replace(validID, updateRequest);

        // confirms the service response contains a non-null object reference
        assertNotNull(result);

        // validates the returned book record matches the new title specified in the
        // request
        assertEquals("Updated Title", result.title());
    }

    @Test
    void missing_resource() {
        // creates a standard UUID string representing an identifier that does not exist
        // in the catalogue
        String missingID = "c9f3a2b1-d4e5-4f6a-8b7c-1a2b3c4d5e6f";

        // instantiates an anonymous inner class implementing the CataloguePort
        // interface to simulate persistence behavior
        CataloguePort port = new CataloguePort() {
            // defines the load method required by the CatalogueService constructor
            @Override
            public List<Book> load() {
                // returns an empty list to simulate a catalogue containing no entries
                return List.of();
            }

            // defines an empty save method to fulfill the interface contract without
            // performing actual disk operations
            @Override
            public void save(List<Book> books) {
            }
        };

        // initializes the target CatalogueService using the empty mock port
        service = new CatalogueService(port);

        // constructs a new request object containing modified fields for the attempted
        // replacement operation
        BookRequest updateRequest = new BookRequest("Updated Title", 1500L, Format.PRINT);

        // verifies that executing the replace method with a missing identifier throws
        // the expected exception
        assertThrows(RuntimeException.class, () -> {
            service.replace(missingID, updateRequest);
        });
    }

    @Test
    void invalid_id() {
        String invalidID = "not-a-valid-uuid";

        CataloguePort port = new CataloguePort() {
            @Override
            public List<Book> load() {
                return List.of(new Book("c9f3a2b1-d4e5-4f6a-8b7c-1a2b3c4d5e6f", "Sample Title", 1000L, Format.PRINT));
            }

            @Override
            public void save(List<Book> books) {
            }
        };

        service = new CatalogueService(port);

        BookRequest updateRequest = new BookRequest("Updated Title", 1500L, Format.PRINT);

        // asserts that calling replace with an invalid identifier triggers an exception
        assertThrows(Exception.class, () -> {
            service.replace(invalidID, updateRequest);
        });
    }

    // null request test
    @Test
    void null_request() {
        String validID = "c9f3a2b1-d4e5-4f6a-8b7c-1a2b3c4d5e6f";

        CataloguePort port = new CataloguePort() {
            @Override
            public List<Book> load() {
                return List.of(new Book(validID, "Sample Title", 1000L, Format.PRINT));
            }

            @Override
            public void save(List<Book> books) {
            }
        };

        service = new CatalogueService(port);

        // asserts that passing a null request object triggers an exception
        assertThrows(Exception.class, () -> {
            service.replace(validID, null);
        });
    }

    // invalid request test
    @Test
    void invalid_request_fields() {
        String validID = "c9f3a2b1-d4e5-4f6a-8b7c-1a2b3c4d5e6f";

        CataloguePort port = new CataloguePort() {
            @Override
            public List<Book> load() {
                return List.of(new Book(validID, "Sample Title", 1000L, Format.PRINT));
            }

            @Override
            public void save(List<Book> books) {
            }
        };

        service = new CatalogueService(port);

        // creates a request with an invalid negative price
        BookRequest invalidRequest = new BookRequest("Updated Title", -500L, Format.PRINT);

        // asserts that invalid fields trigger an exception
        assertThrows(Exception.class, () -> {
            service.replace(validID, invalidRequest);
        });
    }

}
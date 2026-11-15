package edu.cssd2101.lab04;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/** HTTP driving adapter; delegates all catalogue rules to the service. */
@RestController
@RequestMapping("/api/books")
public final class CatalogueController {
    private final CatalogueService service;

    /**
     * Creates the adapter.
     *
     * @param service application operations
     */
    public CatalogueController(CatalogueService service) {
        this.service = service;
    }

    /**
     * Lists books.
     *
     * @return insertion-order JSON array
     */
    @GetMapping
    public List<Book> all() {
        return service.all();
    }

    /**
     * Finds a book.
     *
     * @param id route identifier
     * @return matching entry
     */
    @GetMapping("/{id}")
    public Book find(@PathVariable String id) {
        return service.find(id);
    }

    /**
     * Creates a book.
     *
     * @param request untrusted JSON fields
     * @return 201 with Location and persisted entry
     */
    @PostMapping
    public ResponseEntity<Book> create(@RequestBody BookRequest request) {
        Book created = service.create(request);
        return ResponseEntity.created(URI.create("/api/books/" + created.id())).body(created);
    }

    /**
     * Replaces a book.
     *
     * @param id route identity, never taken from the body
     * @param request complete replacement fields
     * @return persisted entry with unchanged identity
     */
    @PutMapping("/{id}")
    public Book replace(@PathVariable String id, @RequestBody BookRequest request) {
        return service.replace(id, request);
    }

    /**
     * Deletes a book.
     *
     * @param id route identity
     * @return 204 with no response body
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

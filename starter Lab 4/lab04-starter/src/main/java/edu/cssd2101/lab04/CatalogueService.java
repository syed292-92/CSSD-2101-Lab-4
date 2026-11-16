package edu.cssd2101.lab04;

import java.util.*;

/**
 * Application service: validates, builds a candidate, persists it, then
 * publishes it. All public
 * operations use one monitor, serializing this process's writes and reads.
 */
public final class CatalogueService {
    private final CataloguePort port;
    private Map<String, Book> entries = new LinkedHashMap<>();

    /**
     * Restores the catalogue and rejects duplicate identifiers at startup.
     *
     * @param port persistence adapter
     * @throws StorageException if stored entries violate catalogue invariants
     */
    public CatalogueService(CataloguePort port) {
        this.port = Objects.requireNonNull(port);
        try {
            for (Book book : port.load()) {
                Objects.requireNonNull(book);
                if (entries.putIfAbsent(book.id(), book) != null)
                    throw new IllegalArgumentException("Duplicate stored identifier");
            }
        } catch (StorageException failure) {
            throw failure;
        } catch (RuntimeException failure) {
            throw new StorageException(failure);
        }
    }

    /**
     * Returns a stable insertion-order snapshot.
     *
     * @return immutable catalogue snapshot
     */
    public synchronized List<Book> all() {
        return List.copyOf(entries.values());
    }

    /**
     * Retrieves an existing entry.
     *
     * @param id canonical UUID
     * @return matching book
     * @throws MissingBookException     if absent
     * @throws IllegalArgumentException if the identifier is malformed
     */
    public synchronized Book find(String id) {
        Book.validateId(id);
        Book found = entries.get(id);
        if (found == null)
            throw new MissingBookException();
        return found;
    }

    /**
     * Creates an entry with a server-generated identifier.
     *
     * @param request validated during conversion
     * @return persisted new book
     * @throws IllegalArgumentException if the request is invalid
     * @throws StorageException         if persistence fails; state is unchanged
     */
    public synchronized Book create(BookRequest request) {
        if (request == null)
            throw new IllegalArgumentException("Request is required");
        String id;
        do {
            id = UUID.randomUUID().toString();
        } while (entries.containsKey(id));
        Book book = request.toBook(id);
        Map<String, Book> next = new LinkedHashMap<>(entries);
        next.put(id, book);
        commit(next);
        return book;
    }

    /**
     * Replaces all editable fields while preserving identity and ordering.
     *
     * @param id      existing canonical UUID
     * @param request replacement fields
     * @return persisted replacement
     * @throws MissingBookException     if absent
     * @throws IllegalArgumentException if input is invalid
     * @throws StorageException         if persistence fails; state is unchanged
     */
    public synchronized Book replace(String id, BookRequest request) {
        // TODO T2 replacement: follow the task contract in the handout.
        if (id == null) {
            throw new IllegalArgumentException("id is not properly defined");
        }

        // Validates the identifier format to ensure it conforms to required standards.
        Book.validateId(id);

        if (request == null) {
            throw new IllegalArgumentException("request is not properly defined");
        }

        if (!entries.containsKey(id)) {
            throw new MissingBookException();
        }

        // This line changes the request into a book object using the ID.
        Book updatedbBook = request.toBook(id);

        // copies current catalog records into a separate working map to prevent changes
        // to original catologue
        Map<String, Book> candidateMap = new LinkedHashMap<>(entries);

        // Stores the updated book record under the target identifier inside the working
        // copy
        candidateMap.put(id, updatedbBook);

        // Sends the modified working collection to persistence, saving records and
        // updating live memory.
        commit(candidateMap);
        return updatedbBook;
    }

    /**
     * Deletes one existing entry.
     *
     * @param id existing canonical UUID
     * @throws MissingBookException     if absent
     * @throws IllegalArgumentException if malformed
     * @throws StorageException         if persistence fails; state is unchanged
     */
    public synchronized void delete(String id) {
        // TODO T3 deletion: follow the task contract in the handout.
        throw new UnsupportedOperationException("T3 deletion not implemented");
    }

    private void commit(Map<String, Book> next) {
        port.save(List.copyOf(next.values()));
        entries = next;
    }
}
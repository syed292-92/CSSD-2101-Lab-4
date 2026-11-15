package edu.cssd2101.lab04;

import java.util.List;

/**
 * Persistence port for complete catalogue snapshots. A failed save must leave the previous snapshot
 * intact.
 */
public interface CataloguePort {
    /**
     * Loads a snapshot; an absent backing file represents an empty catalogue.
     *
     * @return immutable or independently owned entries
     * @throws StorageException if existing data cannot be read and validated
     */
    List<Book> load();

    /**
     * Atomically replaces the previous snapshot before returning successfully.
     *
     * @param books complete next snapshot, never null
     * @throws StorageException if replacement cannot be completed
     */
    void save(List<Book> books);
}

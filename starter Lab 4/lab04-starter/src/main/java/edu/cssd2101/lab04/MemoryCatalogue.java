package edu.cssd2101.lab04;

import java.util.List;

/** In-memory baseline adapter. Its contents disappear when this process stops. */
public final class MemoryCatalogue implements CataloguePort {
    private List<Book> books = List.of();

    /** Creates an empty transient catalogue. */
    public MemoryCatalogue() {}

    @Override
    public List<Book> load() {
        return books;
    }

    @Override
    public void save(List<Book> books) {
        this.books = List.copyOf(books);
    }
}

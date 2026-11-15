package edu.cssd2101.lab04;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/** Composition root for the local, single-process catalogue server. */
@SpringBootApplication
public class Main {
    /** Creates the Spring configuration root. */
    public Main() {}

    /**
     * Starts the local HTTP server.
     *
     * @param args Spring Boot configuration arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }

    /**
     * Wires the transient baseline adapter; Task T4 replaces it with JSON storage.
     *
     * @param file configured private catalogue file
     * @return transient baseline catalogue service
     */
    @Bean
    public CatalogueService catalogueService(
            @Value("${catalogue.file:./data/catalogue.json}") String file) {
        return new CatalogueService(new MemoryCatalogue());
    }
}

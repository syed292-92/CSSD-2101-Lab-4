### T4 — JSON persistence and adapter wiring

We completed `JsonCatalogue.save()` to write the catalogue to a temporary JSON file, then atomically replace the destination file. Storage failures are wrapped in `StorageException`, and leftover temporary files are cleaned up without hiding the original result.

We updated `Main.catalogueService()` to use `JsonCatalogue` with the configured file path and kept `MemoryCatalogue` available as a test double.

From starter Lab 4\lab04-starter, we ran the T4 tests:

```powershell
.\mvnw.cmd -Dtest=JsonCatalogueTest clean test
```

The test class passed with **13 tests, 0 failures, and 0 errors**. The tests used temporary directories and verified that:

1. A missing file loaded an empty catalogue, and Unicode book titles were saved and loaded correctly.
2. Updates and deletions remained saved when a new service instance loaded the file.
3. Invalid JSON and duplicate stored IDs caused startup failure without changing the file.
4. Filesystem write failures were reported, and temporary files were cleaned up after successful saves and failed moves.
5. Main used the configured JSON file path.
6. Failed creation, replacement, and deletion left both memory and stored data unchanged.

The `FailingCatalogue` helper deliberately failed and threw an exception during saving to check that unsuccessful operations preserved the original series of books. These tests verify JSON persistence and state preservation, while T5 checks the packaged application through HTTP and actual process restarts.

The successful atomic move is the point where the stored catalogue is replaced. This implementation assumes one application process owns the file and writes the entire catalogue on each save. Atomic replacement does not guarantee survival of sudden power loss.

<img width="793" height="242" alt="image" src="https://github.com/user-attachments/assets/553b8b40-e2e1-46dd-a282-d23c2582c7c1" />

T4 persistence tests, all 13 passed.

### T5 — Acceptance and packaged HTTP verification

From starter Lab 4\lab04-starter, I ran the acceptance suite and built the application JAR:

```powershell
.\mvnw.cmd -Pacceptance clean test
.\mvnw.cmd clean verify javadoc:javadoc
```

The acceptance suite passed with 31 tests, 0 failures, 0 errors, and 0 skipped. I then ran the course-provided smoke script against our group’s JAR from the repository root:

```powershell
python ".\starter Lab 4\smoke-http.py" --jar ".\starter Lab 4\lab04-starter\target\lab04-starter-1.0.0.jar"
```

The script used a temporary catalogue file and an operating-system-assigned port for each launch. It verified that:

1. POST returned 201, and GET using the returned Location retrieved the same book.
2. An invalid`PUT returned 400 and left the book unchanged. A valid PUT returned 200 and kept the same book ID.
3. After the first restart, the updated book was still available. DELETE returned 204
4. After the second restart, the catalogue was still empty.

The script ended with PASS packaged HTTP: create/read/replace/delete, invalid-write preservation, two restarts. Unlike a direct service test, this checks that the **packaged JAR actually starts**, serves HTTP requests, and uses the configured JSON storage across restarts.

<img width="934" height="242" alt="test 1" src="https://github.com/user-attachments/assets/71b87076-a3f7-40c3-9a21-24c5a992192b" />

Acceptance suite, all 31 passed

<img width="717" height="253" alt="JAR test" src="https://github.com/user-attachments/assets/0a83686a-753f-4ced-bffb-1acb4fd3277f" />

Maven built the JAR and generated the Javadocs

<img width="1135" height="37" alt="smoke test" src="https://github.com/user-attachments/assets/683e71c6-4208-4722-80b2-41e3e29fbe16" />

Packaged smoke test

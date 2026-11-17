## Task T1: Boundary Contract, Request Trace & State Preservation

### 1. Trace: `POST /api/books` (JSON Tokens to Stored `Book`)
1. **HTTP Reception (`CatalogueController`)**: Spring MVC matches `POST /api/books` with `Content-Type: application/json` and delegates payload deserialization to Jackson.
2. **JSON Token Parsing (`BookRequest`)**:
   - **Unknown Fields Rejected**: Unknown properties (such as an injected `id` field in the JSON body) fail immediately during deserialization.
   - **Strict Numeric Tokens**: Coercion from floating-point tokens (`VALUE_NUMBER_FLOAT`, e.g., `12.99`) to integer `Long` is disabled so fractional cents fail at the parser rather than silently truncating.
   - **Enum & Null Handling**: Only `"PRINT"` and `"EBOOK"` map to `Format`. Omitted or `null` `"priceCents"` remains `null` (`Long`) rather than defaulting to `0L`.
3. **Identity Generation (`CatalogueService.create`)**: The service generates a canonical lower-case `UUID` (clients never supply identity on creation).
4. **DTO Conversion & Invariant Validation (`BookRequest.toBook` -> `Book`)**:
   - `BookRequest.toBook(id)` rejects any `null` fields before constructing `Book`.
   - `Book` validates itself on construction: strips leading/trailing whitespace from `title`, enforces `1–120` non-whitespace characters, validates `priceCents` within `[0, 100_000_000]` CAD cents (stored as primitive `long`), and validates the canonical UUID format.
5. **Candidate Copy & Commit (`CatalogueService` -> `CataloguePort`)**: Under the service monitor, `CatalogueService` copies the current map into a candidate `LinkedHashMap`, inserts the new `Book`, calls `port.save(...)`, and updates the live `entries` reference only after persistence succeeds.
6. **HTTP Response (`CatalogueController` / `ApiErrors`)**:
   - **201 Created**: Returns the stored `Book` JSON representation and a relative `Location` header (`/api/books/{id}`).
   - **400 Bad Request**: Caught by `ApiErrors` and serialized strictly as a two-field JSON object (`code` and `message`) with no stack traces, exception class names, or filesystem paths.

### 2. Why DTO Absence and Domain Validity Are Different Concerns
- **DTO Absence (`BookRequest` uses `Long`)**: Over HTTP/JSON, a client can omit `"priceCents"` or send `"priceCents": null`. If `BookRequest` used primitive `long`, an omitted field would default to `0L`, making a **missing price** indistinguishable from an **explicitly free book (`0` cents)**. Using nullable `Long` preserves the distinction between *absent input* and *zero value* so conversion can reject `null`.
- **Domain Validity (`Book` uses primitive `long`)**: Once validated across the boundary, absence has no place inside the domain model. Every valid `Book` in the catalogue must have a concrete price in `[0, 100_000_000]` cents. Storing primitive `long` inside `Book` makes an unpriced entry unrepresentable in memory across HTTP requests, direct service calls, and disk loading.

### 3. Pre-Run Expected Outcome Matrix (Boundary Cases)

| Test Case / Input Condition | Layer Tested | Expected Status / Outcome | Post-Request Catalogue State |
| :--- | :--- | :--- | :--- |
| `priceCents = 0` (Free book) | Service & HTTP | `201 Created` + relative `Location` | Book stored with `priceCents == 0L` |
| `priceCents = 100000000` (Max price) | Service & HTTP | `201 Created` + relative `Location` | Book stored with `priceCents == 100_000_000L` |
| `title = "  Effective Java  "` | Service & HTTP | `201 Created` | Stored title stripped to `"Effective Java"` |
| `priceCents = null` (Missing price) | Service & HTTP | `400 Bad Request` (`code`, `message`) | Catalogue empty / prior snapshot unchanged |
| `priceCents = 12.99` (Fractional cents) | HTTP Wire | `400 Bad Request` (`code`, `message`) | Catalogue empty / prior snapshot unchanged |
| `priceCents = -1` or `100000001` | Service & HTTP | `400 Bad Request` (`code`, `message`) | Catalogue empty / prior snapshot unchanged |
| `format = "AUDIOBOOK"` (Unknown format) | HTTP Wire | `400 Bad Request` (`code`, `message`) | Catalogue empty / prior snapshot unchanged |
| `title = "   "` (Whitespace-only title) | Service & HTTP | `400 Bad Request` (`code`, `message`) | Catalogue empty / prior snapshot unchanged |
| `id` supplied in request body | HTTP Wire | `400 Bad Request` (`code`, `message`) | Catalogue empty / prior snapshot unchanged |
| Invalid `PUT` body on existing book | Service & HTTP | `400 Bad Request` (`code`, `message`) | Original `Book` snapshot completely unchanged |
| Invalid `PUT` body on missing valid ID | Service & HTTP | `400 Bad Request` (400 precedes 404) | Validation runs before existence check |

### 4. Reproducible Verification Commands & Results (T1)

**Commands Executed (from `lab04-starter/`):**
```powershell
.\mvnw.cmd --version
.\mvnw.cmd test "-Dtest=T1ServiceBoundaryTest,T1HttpBoundaryContractTest"
.\mvnw.cmd clean verify javadoc:javadoc
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 4.637 s -- in edu.cssd2101.lab04.T1HttpBoundaryContractTest
[INFO] Running edu.cssd2101.lab04.T1ServiceBoundaryTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.014 s -- in edu.cssd2101.lab04.T1ServiceBoundaryTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 24, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] 
[INFO] --- jar:3.5.1:jar (default-jar) @ lab04-starter ---
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  24.544 s
[INFO] Finished at: 2026-11-16T19:53:17-05:00
[INFO] ------------------------------------------------------------------------

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

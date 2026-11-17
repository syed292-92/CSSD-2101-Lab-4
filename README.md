### T5 — Acceptance and packaged HTTP verification

From `starter Lab 4\lab04-starter`, I ran the acceptance suite and built the application JAR:

```powershell
.\mvnw.cmd -Pacceptance clean test
.\mvnw.cmd clean verify javadoc:javadoc
```

The acceptance suite passed with **31 tests, 0 failures, 0 errors, and 0 skipped**. I then ran the course-provided smoke script against our group’s JAR from the repository root:

```powershell
python ".\starter Lab 4\smoke-http.py" --jar ".\starter Lab 4\lab04-starter\target\lab04-starter-1.0.0.jar"
```

The script used a temporary catalogue file and an operating-system-assigned port for each launch. It verified that:

1. `POST` returned `201`, and `GET` using the returned `Location` retrieved the same book.
2. An invalid `PUT` returned `400` and left the book unchanged. A valid `PUT` returned `200` and kept the same book ID.
3. After the first restart, the updated book was still available. `DELETE` returned `204`.
4. After the second restart, the catalogue was still empty.

The script ended with `PASS packaged HTTP: create/read/replace/delete, invalid-write preservation, two restarts`. Unlike a direct service test, this checks that the **packaged JAR actually starts**, serves HTTP requests, and uses the configured JSON storage across restarts.<img width="934" height="242" alt="test 1" src="https://github.com/user-attachments/assets/42d24c61-0467-48a9-b7f3-86d5dd581571" />

<img width="934" height="242" alt="test 1" src="https://github.com/user-attachments/assets/71b87076-a3f7-40c3-9a21-24c5a992192b" />

Acceptance suite, all 31 passed

<img width="717" height="253" alt="JAR test" src="https://github.com/user-attachments/assets/0a83686a-753f-4ced-bffb-1acb4fd3277f" />

Maven built the JAR and generated the Javadocs

<img width="1135" height="37" alt="smoke test" src="https://github.com/user-attachments/assets/683e71c6-4208-4722-80b2-41e3e29fbe16" />

Packaged smoke test

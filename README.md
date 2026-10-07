# be-interview-prep

Five Spring Boot features, each delivered as its own branch and pull request.

| # | Question | PR link |
|---|----------|---------|
| 1 | Library API | [PR #1](https://github.com/SaranyaSanil/be-interview-prep/pull/1) |
| 2 | Expense Tracker | [PR #2](https://github.com/SaranyaSanil/be-interview-prep/pull/2) |
| 3 | File Upload Service | [PR #3](https://github.com/SaranyaSanil/be-interview-prep/pull/3) |
| 4 | API Rate Limiting | [PR #4](https://github.com/SaranyaSanil/be-interview-prep/pull/4) |
| 5 | Appointment Booking | |

Video:

## Stack

Java 17, Spring Boot 3.5, Maven (wrapper included), Spring Data JPA, PostgreSQL 17 (Docker Compose),
JUnit 5, Mockito.

## Running locally

Prerequisites: JDK 17+ and Docker.

```bash
cp .env.example .env              # set DB_USERNAME / DB_PASSWORD
docker compose up -d --wait       # PostgreSQL on localhost:5433
./mvnw spring-boot:run            # app on http://localhost:8080 (override with SERVER_PORT)
./mvnw test                       # tests run against the interviewprep_test database
```

Configuration comes from environment variables (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `SERVER_PORT`),
which can be placed in a git-ignored `.env` file. No credentials are committed.

## Q1 — Library API

Base path `/api/books`. All errors use RFC 7807 `ProblemDetail` JSON (`status`, `title`, `detail`, plus `errors`
for field validation).

| Method | Path | Result |
|--------|------|--------|
| POST | `/api/books` | 201 created; 400 invalid; 409 duplicate ISBN |
| GET | `/api/books?title=&author=` | 200, case-insensitive partial match |
| GET | `/api/books/{id}` | 200; 404 |
| PUT | `/api/books/{id}` | 200; 400; 404; 409 duplicate ISBN |
| DELETE | `/api/books/{id}` | 204; 404; 409 if currently borrowed |
| POST | `/api/books/{id}/borrow` `{"memberId":"alice"}` | 200; 404; 409 if already borrowed |
| POST | `/api/books/{id}/return` | 200; 404; 409 if not borrowed |

```bash
curl -X POST localhost:8080/api/books -H 'Content-Type: application/json'   -d '{"title":"Dune","author":"Frank Herbert","isbn":"978-0441013593","publishedYear":1965}'
curl -X POST localhost:8080/api/books/1/borrow -H 'Content-Type: application/json' -d '{"memberId":"alice"}'
```

## Q2 — Expense Tracker

Base path `/api/expenses`. Amount: positive, at most 2 decimal places. Category: `FOOD`, `TRAVEL`, `BILLS`, `OTHER`.
Date: `yyyy-MM-dd`. Note: optional.

| Method | Path | Result |
|--------|------|--------|
| POST | `/api/expenses` | 201 created; 400 invalid |
| GET | `/api/expenses?from=&to=&category=` | 200, sorted by date; all filters optional, combined with AND, dates inclusive |
| GET | `/api/expenses/{id}` | 200; 404 |
| PUT | `/api/expenses/{id}` | 200; 400; 404 |
| DELETE | `/api/expenses/{id}` | 204; 404 |
| GET | `/api/expenses/summary?month=2026-02` | 200, total per category (all four, `0.00` if none) and overall total |

```bash
curl -X POST localhost:8080/api/expenses -H 'Content-Type: application/json'   -d '{"amount":0.10,"category":"FOOD","date":"2026-02-01","note":"Coffee"}'
curl 'localhost:8080/api/expenses/summary?month=2026-02'
```

## Q3 — File Upload Service

Base path `/api/files`. Accepts JPEG, PNG and PDF up to 5 MB, detected from the file's content (magic bytes), not
its name. Files are stored under a generated UUID in `FILE_STORAGE_DIR` (default `./uploads`).

| Method | Path | Result |
|--------|------|--------|
| POST | `/api/files` (multipart field `file`) | 201 metadata; 400 empty/missing; 413 over 5 MB; 415 other types |
| GET | `/api/files` | 200, name, type, size and upload time, newest first |
| GET | `/api/files/{id}` | 200 metadata; 404 |
| GET | `/api/files/{id}/content` | 200 file, downloaded with its original name; 404 |
| DELETE | `/api/files/{id}` | 204, removes file and record; 404 |

```bash
curl -F "file=@photo.png" localhost:8080/api/files
curl -OJ localhost:8080/api/files/{id}/content
```

## Q4 — API Rate Limiting

`GET /api/quotes/random` returns a random quote. Each client must send an `X-API-Key` header and may make at most
`RATE_LIMIT_REQUESTS` (default 10) requests in any sliding `RATE_LIMIT_WINDOW` (default `1m`).

| Case | Result |
|------|--------|
| Within the limit | 200, `X-RateLimit-Remaining` header |
| Missing `X-API-Key` | 401 |
| Over the limit | 429, `Retry-After: <seconds>` header and `retryAfterSeconds` in the body |

```bash
for i in $(seq 1 11); do curl -s -o /dev/null -w "%{http_code}
" -H 'X-API-Key: demo' localhost:8080/api/quotes/random; done
RATE_LIMIT_REQUESTS=3 RATE_LIMIT_WINDOW=10s ./mvnw spring-boot:run   # change the limit without code changes
```


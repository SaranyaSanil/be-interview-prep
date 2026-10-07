# be-interview-prep

Five Spring Boot features, each delivered as its own branch and pull request.

| # | Question | PR link |
|---|----------|---------|
| 1 | Library API | [PR #1](https://github.com/SaranyaSanil/be-interview-prep/pull/1) |
| 2 | Expense Tracker | |
| 3 | File Upload Service | |
| 4 | API Rate Limiting | |
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


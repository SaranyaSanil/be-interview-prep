# be-interview-prep

Five Spring Boot features, each delivered as its own branch and pull request.

| # | Question | PR link |
|---|----------|---------|
| 1 | Library API | |
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

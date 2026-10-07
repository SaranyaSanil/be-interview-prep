package com.interviewprep;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class InterviewPrepApplicationTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void connectsToPostgresTestDatabase() {
        String version = jdbcTemplate.queryForObject("SELECT version()", String.class);
        String database = jdbcTemplate.queryForObject("SELECT current_database()", String.class);

        assertThat(version).startsWith("PostgreSQL");
        assertThat(database).isEqualTo("interviewprep_test");
    }
}

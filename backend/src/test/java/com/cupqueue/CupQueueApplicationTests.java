package com.cupqueue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class CupQueueApplicationTests {

    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17")
            .withDatabaseName("cupqueue_test")
            .withUsername("cupqueue_test")
            .withPassword("cupqueue_test");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    MockMvc mockMvc;

    @Test
    void contextLoads() {
    }

    @Test
    void publishesOpenApiDocument() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").value("3.1.0"))
                .andExpect(jsonPath("$.info.title").value("CupQueue API"))
                .andExpect(jsonPath("$.info.version").value("v1"));
    }

    @Test
    void databaseManagesStoreTimestamps() {
        Long storeId = jdbcTemplate.queryForObject("""
                INSERT INTO stores (name, timezone, tax_rate)
                VALUES (?, ?, ?)
                RETURNING id
                """, Long.class, "Timestamp test store", "UTC", new BigDecimal("0.1300"));

        Instant createdAtBeforeUpdate = jdbcTemplate.queryForObject(
                "SELECT created_at FROM stores WHERE id = ?",
                (resultSet, rowNumber) -> resultSet.getObject(1, OffsetDateTime.class).toInstant(),
                storeId);
        Instant updatedAtBeforeUpdate = jdbcTemplate.queryForObject(
                "SELECT updated_at FROM stores WHERE id = ?",
                (resultSet, rowNumber) -> resultSet.getObject(1, OffsetDateTime.class).toInstant(),
                storeId);
        assertEquals(createdAtBeforeUpdate, updatedAtBeforeUpdate);

        jdbcTemplate.execute("SELECT pg_sleep(0.01)");
        jdbcTemplate.update("UPDATE stores SET name = ? WHERE id = ?", "Updated timestamp test store", storeId);

        Instant createdAtAfterUpdate = jdbcTemplate.queryForObject(
                "SELECT created_at FROM stores WHERE id = ?",
                (resultSet, rowNumber) -> resultSet.getObject(1, OffsetDateTime.class).toInstant(),
                storeId);
        Instant updatedAtAfterUpdate = jdbcTemplate.queryForObject(
                "SELECT updated_at FROM stores WHERE id = ?",
                (resultSet, rowNumber) -> resultSet.getObject(1, OffsetDateTime.class).toInstant(),
                storeId);

        assertEquals(createdAtBeforeUpdate, createdAtAfterUpdate);
        assertTrue(updatedAtAfterUpdate.isAfter(updatedAtBeforeUpdate));
    }

}

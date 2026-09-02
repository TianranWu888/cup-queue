package com.cupqueue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@EnabledIfSystemProperty(named = "cupqueue.test.local-database", matches = "true")
class LocalDatabaseApplicationTests {

    @Test
    void contextLoadsAgainstLocalDatabase() {
    }
}

package com.yourname.gtstracker.database;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DatabaseManagerInitializationFailureTest {

    @Test
    void initializePropagatesFailureAsIllegalStateException() {
        DatabaseManager databaseManager = new DatabaseManager("jdbc:not-a-real-driver:test");

        IllegalStateException thrown = assertThrows(IllegalStateException.class, databaseManager::initialize);

        assertEquals("Failed to initialize SQLite database.", thrown.getMessage());
        assertInstanceOf(java.sql.SQLException.class, thrown.getCause());
        assertNull(databaseManager.getConnection());
    }
}

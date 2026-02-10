package com.example.mod;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExampleModTest {
    @Test
    void modIdIsStable() {
        assertEquals("examplemod", ExampleMod.MOD_ID);
    }
}

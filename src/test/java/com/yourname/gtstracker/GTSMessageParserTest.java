package com.yourname.gtstracker;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class GTSMessageParserTest {

    private static Class<?> parserClass;

    @BeforeAll
    static void loadParser() throws Exception {
        parserClass = Class.forName("com.yourname.gtstracker.GTSMessageParser");
    }

    @Test
    void parsesPokemonSample() throws Exception {
        String message = "[GTS] SELL Pokemon | Species: Gengar | Level: 55 | IV: 31/31/31/31/31/31 | Price: 250000";

        Object parsed = invokeParse(message);
        assertNotNull(parsed, "Pokemon sample should parse into a listing object");

        assertEquals("pokemon", String.valueOf(readField(parsed, "listingType", "type", "category")).toLowerCase());
        assertEquals("gengar", String.valueOf(readField(parsed, "species", "name", "pokemon")).toLowerCase());
        assertTrue(String.valueOf(readField(parsed, "price", "askPrice", "amount")).contains("250000"));
    }

    @Test
    void parsesItemSample() throws Exception {
        String message = "[GTS] SELL Item | Item: Master Ball | Qty: 3 | Price: 90000";

        Object parsed = invokeParse(message);
        assertNotNull(parsed, "Item sample should parse into a listing object");

        assertEquals("item", String.valueOf(readField(parsed, "listingType", "type", "category")).toLowerCase());
        assertEquals("master ball", String.valueOf(readField(parsed, "item", "itemName", "name")).toLowerCase());
        assertTrue(String.valueOf(readField(parsed, "quantity", "qty", "count")).contains("3"));
    }

    @Test
    void rejectsInvalidSample() throws Exception {
        String message = "Totally unrelated chat message that is not a GTS listing";

        Object parsed = invokeParse(message);
        if (parsed instanceof Optional<?>) {
            assertTrue(((Optional<?>) parsed).isEmpty(), "Invalid sample should not produce a listing");
        } else {
            assertNull(parsed, "Invalid sample should return null/empty result");
        }
    }

    private Object invokeParse(String message) throws Exception {
        Method parseMethod = Arrays.stream(parserClass.getDeclaredMethods())
                .filter(m -> m.getName().toLowerCase().contains("parse"))
                .filter(m -> m.getParameterCount() == 1)
                .filter(m -> m.getParameterTypes()[0] == String.class)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No parse(String) method found on GTSMessageParser"));

        parseMethod.setAccessible(true);
        return parseMethod.invoke(null, message);
    }

    private Object readField(Object target, String... candidateNames) {
        if (target == null) {
            return null;
        }

        if (target instanceof Optional<?>) {
            Optional<?> optional = (Optional<?>) target;
            if (optional.isEmpty()) {
                return null;
            }
            return readField(optional.get(), candidateNames);
        }

        if (target instanceof Map<?, ?> map) {
            for (String name : candidateNames) {
                if (map.containsKey(name)) {
                    return map.get(name);
                }
            }
        }

        Class<?> type = target.getClass();

        if (type.isRecord()) {
            for (RecordComponent component : type.getRecordComponents()) {
                for (String name : candidateNames) {
                    if (component.getName().equalsIgnoreCase(name)) {
                        try {
                            return component.getAccessor().invoke(target);
                        } catch (Exception ignored) {
                            // fall through to other strategies
                        }
                    }
                }
            }
        }

        for (String name : candidateNames) {
            try {
                Method getter = type.getMethod("get" + Character.toUpperCase(name.charAt(0)) + name.substring(1));
                return getter.invoke(target);
            } catch (Exception ignored) {
                // continue searching
            }
            try {
                Method getter = type.getMethod(name);
                return getter.invoke(target);
            } catch (Exception ignored) {
                // continue searching
            }
        }

        fail("Could not read any of fields " + Arrays.toString(candidateNames) + " from " + type.getName());
        return null;
    }
}

package com.yourname.gtstracker;

import com.yourname.gtstracker.chat.GTSChatMonitor;
import com.yourname.gtstracker.config.ConfigModel;
import com.yourname.gtstracker.database.DatabaseManager;
import com.yourname.gtstracker.ingest.ListingIngestionService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GTSTrackerModInitializationTest {

    @Test
    void onInitializeClient_wiresStartupSequenceInCanonicalOrder() {
        List<String> events = new ArrayList<>();

        ConfigModel config = new ConfigModel();
        FakeDatabaseManager db = new FakeDatabaseManager(events, true);
        FakeChatMonitor chatMonitor = new FakeChatMonitor(events);

        GTSTrackerMod mod = new GTSTrackerMod(
            () -> {
                events.add("config.load");
                return config;
            },
            () -> {
                events.add("database.new");
                return db;
            },
            manager -> {
                events.add("ingestion.new");
                return new ListingIngestionService(manager);
            },
            (ingestion, cfg) -> {
                events.add("chatmonitor.new");
                assertSame(config, cfg);
                assertNotNull(ingestion);
                return chatMonitor;
            },
            () -> events.add("compatibility.log"),
            () -> events.add("command.register"),
            new RecordingStartupLogger(events)
        );

        mod.onInitializeClient();

        assertSame(mod, GTSTrackerMod.getInstance());
        assertSame(config, mod.getConfig());
        assertSame(db, mod.getDatabaseManager());
        assertNotNull(mod.getIngestionService());

        assertEquals(
            List.of(
                "startup.begin",
                "config.load",
                "database.new",
                "database.initialize",
                "ingestion.new",
                "chatmonitor.new",
                "chatmonitor.register",
                "compatibility.log",
                "command.register",
                "startup.success"
            ),
            events
        );
    }

    @Test
    void onInitializeClient_logsAndThrowsOnceOnFatalStartupFailure() {
        List<String> events = new ArrayList<>();
        AtomicInteger failureCount = new AtomicInteger();
        RuntimeException expected = new RuntimeException("boom");

        GTSTrackerMod mod = new GTSTrackerMod(
            () -> {
                events.add("config.load");
                return new ConfigModel();
            },
            () -> {
                events.add("database.new");
                return new FakeDatabaseManager(events, true);
            },
            databaseManager -> {
                events.add("ingestion.new");
                return new ListingIngestionService(databaseManager);
            },
            (ingestion, config) -> {
                events.add("chatmonitor.new");
                throw expected;
            },
            () -> events.add("compatibility.log"),
            () -> events.add("command.register"),
            new GTSTrackerMod.StartupLogger() {
                @Override
                public void startupBegin() {
                    events.add("startup.begin");
                }

                @Override
                public void startupSuccess() {
                    events.add("startup.success");
                }

                @Override
                public void startupFailure(RuntimeException exception) {
                    events.add("startup.failure");
                    assertSame(expected, exception);
                    failureCount.incrementAndGet();
                }
            }
        );

        RuntimeException thrown = assertThrows(RuntimeException.class, mod::onInitializeClient);

        assertSame(expected, thrown);
        assertEquals(1, failureCount.get());
        assertEquals(
            List.of(
                "startup.begin",
                "config.load",
                "database.new",
                "database.initialize",
                "ingestion.new",
                "chatmonitor.new",
                "startup.failure"
            ),
            events
        );
    }

    private static final class RecordingStartupLogger implements GTSTrackerMod.StartupLogger {
        private final List<String> events;

        private RecordingStartupLogger(List<String> events) {
            this.events = events;
        }

        @Override
        public void startupBegin() {
            events.add("startup.begin");
        }

        @Override
        public void startupSuccess() {
            events.add("startup.success");
        }

        @Override
        public void startupFailure(RuntimeException exception) {
            events.add("startup.failure");
        }
    }

    private static final class FakeDatabaseManager extends DatabaseManager {
        private final List<String> events;
        private final boolean connected;

        private FakeDatabaseManager(List<String> events, boolean connected) {
            this.events = events;
            this.connected = connected;
        }

        @Override
        public void initialize() {
            events.add("database.initialize");
        }

        @Override
        public Connection getConnection() {
            if (!connected) {
                return null;
            }
            return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("toString")) {
                        return "FakeConnection";
                    }
                    Class<?> type = method.getReturnType();
                    if (type.equals(boolean.class)) {
                        return false;
                    }
                    if (type.equals(int.class)) {
                        return 0;
                    }
                    if (type.equals(long.class)) {
                        return 0L;
                    }
                    if (type.equals(float.class)) {
                        return 0f;
                    }
                    if (type.equals(double.class)) {
                        return 0d;
                    }
                    return null;
                }
            );
        }
    }

    private static final class FakeChatMonitor extends GTSChatMonitor {
        private final List<String> events;

        private FakeChatMonitor(List<String> events) {
            super(new ListingIngestionService(new DatabaseManager()), new ConfigModel());
            this.events = events;
        }

        @Override
        public void register() {
            events.add("chatmonitor.register");
        }
    }
}

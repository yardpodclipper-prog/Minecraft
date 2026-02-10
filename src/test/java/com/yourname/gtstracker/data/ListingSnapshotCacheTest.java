package com.yourname.gtstracker.data;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ListingSnapshotCacheTest {
    @Test
    void shutdownClosesOwnedExecutorAndPreventsFurtherRefreshWork() throws Exception {
        AtomicInteger providerCalls = new AtomicInteger(0);
        ListingSnapshotCache cache = new ListingSnapshotCache(() -> {
            providerCalls.incrementAndGet();
            return ListingSnapshot.empty();
        });

        cache.refreshIfStale(System.currentTimeMillis(), 0);
        waitForProviderCalls(providerCalls, 1);

        cache.shutdown();

        Field ownedExecutorField = ListingSnapshotCache.class.getDeclaredField("ownedExecutor");
        ownedExecutorField.setAccessible(true);
        ExecutorService ownedExecutor = (ExecutorService) ownedExecutorField.get(cache);

        assertNotNull(ownedExecutor);
        assertTrue(ownedExecutor.isShutdown(), "Expected owned executor to be shut down after cache.shutdown().");

        cache.refreshIfStale(System.currentTimeMillis(), 0);
        Thread.sleep(100L);

        assertEquals(1, providerCalls.get(), "No additional provider refresh should run after shutdown.");
    }

    private static void waitForProviderCalls(AtomicInteger calls, int expectedCalls) throws InterruptedException {
        long timeoutMs = 2_000L;
        long started = System.currentTimeMillis();
        while (calls.get() < expectedCalls && System.currentTimeMillis() - started < timeoutMs) {
            Thread.sleep(10L);
        }
        assertEquals(expectedCalls, calls.get(), "Timed out waiting for async refresh execution.");
    }
}

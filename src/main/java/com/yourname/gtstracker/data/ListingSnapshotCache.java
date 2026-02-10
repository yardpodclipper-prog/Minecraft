package com.yourname.gtstracker.data;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Offloads data retrieval from the render loop and keeps the latest loaded snapshot in memory.
 */
public final class ListingSnapshotCache {
    private final ListingSnapshotProvider provider;
    private final Executor executor;

    private volatile ListingSnapshot snapshot = ListingSnapshot.empty();
    private volatile CompletableFuture<?> refreshTask;
    private volatile long lastRefreshMs;

    public ListingSnapshotCache(ListingSnapshotProvider provider) {
        this(provider, Executors.newSingleThreadExecutor());
    }

    public ListingSnapshotCache(ListingSnapshotProvider provider, Executor executor) {
        this.provider = provider;
        this.executor = executor;
    }

    public ListingSnapshot getSnapshot() {
        return snapshot;
    }

    public void refreshIfStale(long nowMs, long refreshIntervalMs) {
        if (refreshTask != null && !refreshTask.isDone()) {
            return;
        }

        if (nowMs - lastRefreshMs < refreshIntervalMs) {
            return;
        }

        refreshTask = CompletableFuture
            .supplyAsync(provider::fetchSnapshot, executor)
            .thenAccept(loaded -> {
                snapshot = loaded == null ? ListingSnapshot.empty() : loaded;
                lastRefreshMs = nowMs;
            })
            .exceptionally(ex -> {
                lastRefreshMs = nowMs;
                return null;
            });
    }
}

package com.yourname.gtstracker.data;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Offloads data retrieval from the render loop and keeps the latest loaded snapshot in memory.
 */
public final class ListingSnapshotCache implements AutoCloseable {
    private final ListingSnapshotProvider provider;
    private final Executor executor;
    private final ExecutorService ownedExecutor;

    private final AtomicBoolean closed = new AtomicBoolean(false);

    private volatile ListingSnapshot snapshot = ListingSnapshot.empty();
    private volatile CompletableFuture<?> refreshTask;
    private volatile long lastRefreshMs;

    public ListingSnapshotCache(ListingSnapshotProvider provider) {
        this(provider, Executors.newSingleThreadExecutor(), true);
    }

    public ListingSnapshotCache(ListingSnapshotProvider provider, Executor executor) {
        this(provider, executor, false);
    }

    private ListingSnapshotCache(ListingSnapshotProvider provider, Executor executor, boolean ownsExecutor) {
        this.provider = provider;
        this.executor = executor;
        this.ownedExecutor = ownsExecutor && executor instanceof ExecutorService ? (ExecutorService) executor : null;
    }

    public ListingSnapshot getSnapshot() {
        return snapshot;
    }

    public boolean isClosed() {
        return closed.get();
    }

    public void refreshIfStale(long nowMs, long refreshIntervalMs) {
        if (isClosed()) {
            return;
        }

        if (refreshTask != null && !refreshTask.isDone()) {
            return;
        }

        if (nowMs - lastRefreshMs < refreshIntervalMs) {
            return;
        }

        refreshTask = CompletableFuture
            .supplyAsync(provider::fetchSnapshot, executor)
            .thenAccept(loaded -> {
                if (isClosed()) {
                    return;
                }
                snapshot = loaded == null ? ListingSnapshot.empty() : loaded;
                lastRefreshMs = nowMs;
            })
            .exceptionally(ex -> {
                if (!isClosed()) {
                    lastRefreshMs = nowMs;
                }
                return null;
            });
    }

    public void shutdown() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }

        CompletableFuture<?> task = refreshTask;
        if (task != null) {
            task.cancel(true);
        }

        if (ownedExecutor != null) {
            ownedExecutor.shutdown();
            try {
                if (!ownedExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                    ownedExecutor.shutdownNow();
                }
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
                ownedExecutor.shutdownNow();
            }
        }
    }

    @Override
    public void close() {
        shutdown();
    }
}

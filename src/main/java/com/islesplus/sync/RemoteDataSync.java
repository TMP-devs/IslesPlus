package com.islesplus.sync;

import com.islesplus.features.nodealertmanager.NodeRepository;
import com.islesplus.features.ownerdecorator.OwnerRepository;
import com.islesplus.features.plushiefinder.PlushieRepository;
import com.islesplus.features.rankcalculator.RiftRepository;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public final class RemoteDataSync {
    private static final AtomicInteger THREAD_ID = new AtomicInteger(1);
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "RemoteDataSync-" + THREAD_ID.getAndIncrement());
        t.setDaemon(true);
        return t;
    });

    private RemoteDataSync() {}

    /** Refreshes all remote/GitHub-backed datasets. Returns true if at least one refresh started. */
    public static boolean refreshNowAsync() {
        boolean plushies = PlushieRepository.refreshRemoteDataNowAsync();
        boolean rifts = RiftRepository.refreshRemoteDataNowAsync();
        boolean nodes = NodeRepository.refreshRemoteDataNowAsync();
        boolean owners = OwnerRepository.refreshRemoteDataNowAsync();
        boolean flags = FeatureFlags.refreshRemoteDataNowAsync();
        return plushies || rifts || nodes || owners || flags;
    }

    /**
     * Fetches all datasets synchronously in parallel. Returns true only if ALL succeed.
     * Call from a background thread only - this blocks until all fetches complete.
     */
    public static boolean refreshAllSync() {
        CompletableFuture<Boolean> plushies = CompletableFuture.supplyAsync(PlushieRepository::refreshSync, EXECUTOR);
        CompletableFuture<Boolean> rifts = CompletableFuture.supplyAsync(RiftRepository::refreshSync, EXECUTOR);
        CompletableFuture<Boolean> nodes = CompletableFuture.supplyAsync(NodeRepository::refreshSync, EXECUTOR);
        CompletableFuture<Boolean> owners = CompletableFuture.supplyAsync(OwnerRepository::refreshSync, EXECUTOR);
        CompletableFuture<Boolean> flags = CompletableFuture.supplyAsync(FeatureFlags::refreshSync, EXECUTOR);

        CompletableFuture.allOf(plushies, rifts, nodes, owners, flags).join();

        return plushies.join() && rifts.join() && nodes.join() && owners.join() && flags.join();
    }
}

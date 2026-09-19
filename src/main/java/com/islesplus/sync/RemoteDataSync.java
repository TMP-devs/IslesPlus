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

    /** kicks off a refresh of everything we pull from github. true if at least one actually started */
    public static boolean refreshNowAsync() {
        boolean plushies = PlushieRepository.refreshRemoteDataNowAsync();
        boolean rifts = RiftRepository.refreshRemoteDataNowAsync();
        boolean nodes = NodeRepository.refreshRemoteDataNowAsync();
        boolean owners = OwnerRepository.refreshRemoteDataNowAsync();
        boolean flags = FeatureFlags.refreshRemoteDataNowAsync();
        return plushies || rifts || nodes || owners || flags;
    }

    /**
     * fetches everything in parallel and blocks until they're all done. true only if ALL succeed.
     * don't call this on the render thread
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

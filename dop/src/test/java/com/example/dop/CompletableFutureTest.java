package com.example.dop;


import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

public class CompletableFutureTest {
    int compute(int n) {
        if ( n <= 0) {
            throw new RuntimeException("Invalid input");
        }

        return n * 2;
    }

    CompletableFuture<Integer> create(int n) {
        return CompletableFuture.supplyAsync(() -> compute(n));
    }

    CompletableFuture<Integer> create() {
        var cf = new CompletableFuture<Integer>();

        cf.thenApply(data -> data * 10);
        cf.thenApply(data -> data + 1);

        return cf;
    }

    @Test
    public void testCompletableFuture() {

        // CompletableFuture non-blocking never stop
        create(-4)
                .thenApply(data -> data + 1.0)
                .thenAccept(System.out::println)
                .exceptionally(ex -> {
                    System.out.println("Oops: " + ex.getMessage());
                    throw new RuntimeException("Oops: " + ex.getMessage());
                })
                .thenRun(() -> System.out.println("log some info .."))
                .thenRun(() -> System.out.println("some post op .."))
                ;

        System.out.println("Started the computation");
    }

    @Test
    public void testMultipleCompletableFuture() {
        var cf1 = create(2);
        var cf2 = create(3);
        cf1.thenCombine(cf2, (data1, data2) -> data1 + data2)
                .thenAccept(System.out::println);
    }

    @Test
    public void testMultipleCompletableFuture2() {
        create(2)
                .thenCompose(data -> create(data))
                .thenAccept(System.out::println);
    }

    @Test
    public void testCompletableFuture2() {
        var cf = create();
        cf.complete(10);

        try { Thread.sleep(1000); } catch (InterruptedException e) {}
    }
}

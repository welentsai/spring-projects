package com.example.dop;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

public class CompletableFutureTest {

    int compute(int n) {
        if (n <= 0) {
            throw new RuntimeException("Invalid input");
        }

        try {
            TimeUnit.SECONDS.sleep(1);
            return n * 2;
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    CompletableFuture<Integer> create(int n) {
        return CompletableFuture.supplyAsync(() -> compute(n));
    }

    CompletableFuture<Integer> create() {
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }

        var cf = new CompletableFuture<Integer>();
        cf.thenApply(data -> data * 10);
        cf.thenApply(data -> data + 1);
        cf.thenApply(data -> {
            System.out.println("serialized: " + data);
            return data;
        });

        return cf;
    }

    @Test
    public void testCompletableFuture() {
        // CompletableFuture non-blocking never stop
        create(-4).thenApply(data -> data + 1.0).thenAccept(System.out::println).exceptionally(ex -> {
            System.out.println("Oops: " + ex.getMessage());
            throw new RuntimeException("Oops: " + ex.getMessage());
        }).thenRun(() -> System.out.println("log some info ..")).thenRun(() -> System.out.println("some post op .."));

        System.out.println("Started the computation");
    }

    @Test
    public void testThenCombine() {
        var cf1 = create(2);
        var cf2 = create(3);

        // combine 就是 parzip, will wait cf1 and cf2
        cf1.thenCombine(cf2, (data1, data2) -> data1 + data2).thenAccept(System.out::println);
    }

    @Test
    public void testThenCompose() throws ExecutionException, InterruptedException {
        //  還需要了解, 為什麼 thenCompose 就是 map
        CompletableFuture<Integer> composedFuture = create(2).thenCompose(data -> create(data));

        //
        CompletableFuture<Void> acceptFuture = composedFuture.thenAccept(System.out::println);
        System.out.println("thenAccept() called");

        // Wait for completion
        acceptFuture.join();

        System.out.println("All done");
    }

    @Test
    public void testComplete() {
        var cf = create();
        cf.complete(10); // It doesn't start or trigger the computation, complete the CompletableFuture with a specific result
        cf.thenAccept(System.out::println);

        System.out.println("testComplete Done !");
    }

    @Test
    public void testHelloFuture() throws InterruptedException {
        // Create CompletableFuture
        CompletableFuture<String> helloFuture = CompletableFuture.supplyAsync(() -> {
            try {
                TimeUnit.SECONDS.sleep(1);
                return "Hello";

            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        });

        // Chain dependent actions
        CompletableFuture<String> result = helloFuture.thenApply(s -> s + " World").thenApply(String::toUpperCase);

        // Handle the result
//        result.thenAccept(System.out::println);

        try {
            var completedFuture = result.get(5, TimeUnit.SECONDS);
            System.out.println("result: " + completedFuture);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    public void testRunAsync() throws ExecutionException, InterruptedException, TimeoutException {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        CompletableFuture
                .runAsync(() -> { // runAsync() start the execution of a CompletableFuture immediately
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    System.out.println("Hello");
                }, executor)
                .thenRun(() -> {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    System.out.println("World");
                });
//                .get(5, TimeUnit.SECONDS); // get() will wait for the result

        System.out.println("All done");

        Executor executor2 = runnable -> SwingUtilities.invokeLater(runnable);
    }

    private String processString(String input) {
        try {
            Thread.sleep(100);  // Simulate processing time
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return input.toUpperCase();
    }

    @Test
    @DisplayName("""
        Test Stream  with CompletableFuture
    """)
    public void testStreamWithCompletableFuture() throws InterruptedException, ExecutionException {
        CompletableFuture<String> closing = CompletableFuture.supplyAsync(() -> "");
        Stream<String> stringStream = Stream.of("one", "two", "three");

        var reduce = stringStream
                .map(str -> CompletableFuture.supplyAsync(() -> processString(str)))
                .reduce(
                        closing,
                        (cf1, cf2) -> cf1.thenCombine(cf2, (a, b) -> a.isEmpty() ? b : a + "," + b)
                );

        System.out.println("reduce: " + reduce.get());
        System.out.println("testStream2 Done");
    }
}

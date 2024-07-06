package com.example.dop;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Paths;

public class VirtualThreadTest {

    public void fetch(int index) {
        System.out.println("entering " + index + " " + Thread.currentThread());

        try {
            var length = Files.lines(Paths.get("VirtualThreadTest.java")).count();
            System.out.println("result " + index + " " + Thread.currentThread());
        } catch (Exception e) {
            System.out.println("catch " + index + " " + Thread.currentThread());
        } finally {
            System.out.println("finally " + index + " " + Thread.currentThread());
        }
    }

    @Test
    public void testVirtualThread() {
        long startTime = System.currentTimeMillis();
        int MAX = 1000;
        for (int i = 0; i < MAX; i++) {
            int index = i;
            Thread.startVirtualThread(() -> fetch(index));
        }

        try {Thread.sleep(5000);} catch (InterruptedException e) {}
    }
}

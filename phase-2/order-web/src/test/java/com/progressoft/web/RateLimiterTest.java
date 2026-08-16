package com.progressoft.web;


import org.junit.jupiter.api.RepeatedTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RateLimiterTest {

    @RepeatedTest(20)
    void rateLimiterEnforcesLimitUnderConcurrentLoad() throws InterruptedException {
        int maxRequests = 5;
        int totalAttempts = 20;
        RateLimiter limiter = new RateLimiter(maxRequests, 60_000);

        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(totalAttempts);

        AtomicInteger allowed = new AtomicInteger(0);
        AtomicInteger rejected = new AtomicInteger(0);

        for (int i = 0; i < totalAttempts; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    if (limiter.allowRequest("test-client")) {
                        allowed.incrementAndGet();
                    } else {
                        rejected.incrementAndGet();
                    }
                } catch (InterruptedException ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        assertEquals(maxRequests, allowed.get());
        assertEquals(totalAttempts - maxRequests, rejected.get());
    }
}
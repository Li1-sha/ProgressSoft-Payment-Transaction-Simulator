package com.progressoft.web;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class RateLimiterBoundaryTest {

    @Test
    void slidingWindowResetBehavesCorrectlyAtBoundary() throws InterruptedException {
        int maxRequests = 3;
        long windowMillis = 100; // 100 ms window for testing

        RateLimiter limiter = new RateLimiter(maxRequests, windowMillis);
        String client = "test-client";

        // Send max requests within window
        for (int i = 0; i < maxRequests; i++) {
            assertTrue(limiter.allowRequest(client));
        }
        // Next request should be rejected
        assertFalse(limiter.allowRequest(client));

        // Wait for window to expire
        Thread.sleep(windowMillis + 10);

        // After reset, should allow requests again
        for (int i = 0; i < maxRequests; i++) {
            assertTrue(limiter.allowRequest(client));
        }
        assertFalse(limiter.allowRequest(client));

        // --- Concurrent test with a long window (no reset) ---
        // Use a new RateLimiter instance and keep it final.
        final RateLimiter longWindowLimiter = new RateLimiter(maxRequests, 5000);
        final String testClient = "test-client-concurrent";

        int totalAttempts = 20;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger allowed = new AtomicInteger(0);
        AtomicInteger rejected = new AtomicInteger(0);

        for (int i = 0; i < totalAttempts; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    if (longWindowLimiter.allowRequest(testClient)) {
                        allowed.incrementAndGet();
                    } else {
                        rejected.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        startLatch.countDown();
        Thread.sleep(1000); // let all threads finish
        executor.shutdown();

        // Since window is long, allowed should be exactly maxRequests (3), the rest rejected.
        assertEquals(maxRequests, allowed.get());
        assertEquals(totalAttempts - maxRequests, rejected.get());

        // The reset logic is inside a synchronized block, so it's thread-safe.
        // The potential risk is that a thread might read the windowStart before another
        // thread resets it, but because the reset and count reset are inside the same
        // synchronized block, only one thread can execute the reset at a time.
        // We consider this acceptable.
    }
}
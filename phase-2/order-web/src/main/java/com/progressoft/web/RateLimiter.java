package com.progressoft.web;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class RateLimiter {
    private final int maxRequests;
    private final long windowMillis;
    private final ConcurrentHashMap<String, SlidingWindow> counters = new ConcurrentHashMap<>();

    public RateLimiter(int maxRequests, long windowMillis) {
        this.maxRequests = maxRequests;
        this.windowMillis = windowMillis;
    }

    public boolean allowRequest(String clientId) {
        long now = System.currentTimeMillis();
        SlidingWindow window = counters.computeIfAbsent(clientId, k -> new SlidingWindow());
        return window.tryAllow(now);
    }

    private class SlidingWindow {
        private final AtomicInteger count = new AtomicInteger(0);
        private final AtomicLong windowStart = new AtomicLong(System.currentTimeMillis());

        boolean tryAllow(long now) {
            long start = windowStart.get();
            if (now - start > windowMillis) {
                synchronized (this) {
                    if (now - windowStart.get() > windowMillis) {
                        count.set(0);
                        windowStart.set(now);
                    }
                }
            }
            int current = count.get();
            if (current < maxRequests) {
                return count.incrementAndGet() <= maxRequests;
            }
            return false;
        }
    }

    // Cleanup old entries to prevent memory leak
    public void cleanup() {
        long cutoff = System.currentTimeMillis() - windowMillis;
        counters.entrySet().removeIf(e -> e.getValue().windowStart.get() < cutoff);
    }
}
package cloud.sta.stashield.protection;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RateLimiterTest {
    @Test
    void countsOnlyEventsInsideWindow() {
        RateLimiter limiter = new RateLimiter();

        assertEquals(1, limiter.record("203.0.113.10", 1000L, 1000L));
        assertEquals(2, limiter.record("203.0.113.10", 1500L, 1000L));
        assertEquals(2, limiter.record("203.0.113.10", 2101L, 1000L));
    }

    @Test
    void separatesIpWindows() {
        RateLimiter limiter = new RateLimiter();

        assertEquals(1, limiter.record("203.0.113.10", 1000L, 1000L));
        assertEquals(1, limiter.record("203.0.113.11", 1001L, 1000L));
    }
}

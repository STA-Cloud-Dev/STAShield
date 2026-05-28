package cloud.sta.stashield.protection;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class RateLimiter {
    private final ConcurrentMap<String, Window> windows = new ConcurrentHashMap<String, Window>();

    public int record(String key, long nowMillis, long windowMillis) {
        Window window = windows.get(key);
        if (window == null) {
            Window created = new Window();
            Window previous = windows.putIfAbsent(key, created);
            window = previous == null ? created : previous;
        }
        return window.record(nowMillis, windowMillis);
    }

    private static final class Window {
        private final Deque<Long> timestamps = new ArrayDeque<Long>();

        int record(long nowMillis, long windowMillis) {
            synchronized (timestamps) {
                timestamps.addLast(Long.valueOf(nowMillis));
                trim(nowMillis, windowMillis);
                return timestamps.size();
            }
        }

        private void trim(long nowMillis, long windowMillis) {
            long cutoff = nowMillis - windowMillis;
            while (!timestamps.isEmpty() && timestamps.peekFirst().longValue() < cutoff) {
                timestamps.removeFirst();
            }
        }
    }
}

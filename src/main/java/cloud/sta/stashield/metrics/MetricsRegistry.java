package cloud.sta.stashield.metrics;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicLong;

public final class MetricsRegistry {
    private final AtomicLong totalConnections = new AtomicLong();
    private final AtomicLong activeConnections = new AtomicLong();
    private final AtomicLong blockedConnections = new AtomicLong();
    private final AtomicLong allowedConnections = new AtomicLong();
    private final AtomicLong statusPings = new AtomicLong();
    private final AtomicLong loginConnections = new AtomicLong();
    private final AtomicLong backendFailures = new AtomicLong();
    private final AtomicLong peakCps = new AtomicLong();
    private final Deque<Long> recentConnections = new ArrayDeque<Long>();

    public long recordConnectionAccepted(long nowMillis) {
        totalConnections.incrementAndGet();
        activeConnections.incrementAndGet();
        synchronized (recentConnections) {
            recentConnections.addLast(Long.valueOf(nowMillis));
            trim(nowMillis);
            long currentCps = recentConnections.size();
            updatePeakCps(currentCps);
            return currentCps;
        }
    }

    public void recordConnectionClosed() {
        activeConnections.decrementAndGet();
    }

    public void recordBlocked() {
        blockedConnections.incrementAndGet();
    }

    public void recordAllowed() {
        allowedConnections.incrementAndGet();
    }

    public void recordStatusPing() {
        statusPings.incrementAndGet();
    }

    public void recordLoginConnection() {
        loginConnections.incrementAndGet();
    }

    public void recordBackendFailure() {
        backendFailures.incrementAndGet();
    }

    public long currentConnections() {
        return activeConnections.get();
    }

    public Snapshot snapshot() {
        long currentCps;
        long now = System.currentTimeMillis();
        synchronized (recentConnections) {
            trim(now);
            currentCps = recentConnections.size();
        }
        return new Snapshot(
                totalConnections.get(),
                activeConnections.get(),
                blockedConnections.get(),
                allowedConnections.get(),
                statusPings.get(),
                loginConnections.get(),
                backendFailures.get(),
                currentCps,
                peakCps.get());
    }

    private void trim(long nowMillis) {
        long cutoff = nowMillis - 1000L;
        while (!recentConnections.isEmpty() && recentConnections.peekFirst().longValue() < cutoff) {
            recentConnections.removeFirst();
        }
    }

    private void updatePeakCps(long currentCps) {
        long observed;
        do {
            observed = peakCps.get();
            if (currentCps <= observed) {
                return;
            }
        } while (!peakCps.compareAndSet(observed, currentCps));
    }

    public static final class Snapshot {
        public final long totalConnections;
        public final long activeConnections;
        public final long blockedConnections;
        public final long allowedConnections;
        public final long statusPings;
        public final long loginConnections;
        public final long backendFailures;
        public final long currentCps;
        public final long peakCps;

        private Snapshot(long totalConnections,
                         long activeConnections,
                         long blockedConnections,
                         long allowedConnections,
                         long statusPings,
                         long loginConnections,
                         long backendFailures,
                         long currentCps,
                         long peakCps) {
            this.totalConnections = totalConnections;
            this.activeConnections = activeConnections;
            this.blockedConnections = blockedConnections;
            this.allowedConnections = allowedConnections;
            this.statusPings = statusPings;
            this.loginConnections = loginConnections;
            this.backendFailures = backendFailures;
            this.currentCps = currentCps;
            this.peakCps = peakCps;
        }
    }
}

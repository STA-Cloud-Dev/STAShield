package cloud.sta.stashield.metrics;

import cloud.sta.stashield.config.StaShieldConfig;
import cloud.sta.stashield.log.StaLogger;
import cloud.sta.stashield.util.NamedThreadFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class MetricsReporter {
    private final StaShieldConfig config;
    private final MetricsRegistry metrics;
    private final StaLogger logger;
    private ScheduledExecutorService executor;

    public MetricsReporter(StaShieldConfig config, MetricsRegistry metrics, StaLogger logger) {
        this.config = config;
        this.metrics = metrics;
        this.logger = logger;
    }

    public void start() {
        if (!config.metrics.enabled) {
            return;
        }
        executor = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("stashield-metrics", true));
        executor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                report();
            }
        }, config.metrics.logIntervalSeconds, config.metrics.logIntervalSeconds, TimeUnit.SECONDS);
    }

    public void stop() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    private void report() {
        MetricsRegistry.Snapshot snapshot = metrics.snapshot();
        logger.info("metrics connections=" + snapshot.totalConnections
                + " active=" + snapshot.activeConnections
                + " allowed=" + snapshot.allowedConnections
                + " blocked=" + snapshot.blockedConnections
                + " status=" + snapshot.statusPings
                + " login=" + snapshot.loginConnections
                + " backend_failures=" + snapshot.backendFailures
                + " cps=" + snapshot.currentCps
                + " peak_cps=" + snapshot.peakCps);
    }
}

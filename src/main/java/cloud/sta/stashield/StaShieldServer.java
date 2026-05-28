package cloud.sta.stashield;

import cloud.sta.stashield.config.StaShieldConfig;
import cloud.sta.stashield.log.StaLogger;
import cloud.sta.stashield.metrics.MetricsRegistry;
import cloud.sta.stashield.metrics.MetricsReporter;
import cloud.sta.stashield.network.ProxyListener;
import cloud.sta.stashield.protection.ConnectionGuard;
import cloud.sta.stashield.util.NamedThreadFactory;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public final class StaShieldServer {
    private final StaShieldConfig config;
    private final StaLogger logger;
    private final MetricsRegistry metrics;
    private final MetricsReporter metricsReporter;
    private final ConnectionGuard connectionGuard;
    private final ExecutorService executor;
    private final AtomicBoolean stopped = new AtomicBoolean(false);
    private ProxyListener listener;

    public StaShieldServer(StaShieldConfig config, StaLogger logger) {
        this.config = config;
        this.logger = logger;
        this.metrics = new MetricsRegistry();
        this.metricsReporter = new MetricsReporter(config, metrics, logger);
        this.connectionGuard = new ConnectionGuard(config);
        this.executor = Executors.newCachedThreadPool(new NamedThreadFactory("stashield-io", false));
    }

    public void start() throws IOException {
        logger.info("Starting STAShield backend=" + config.backend.host + ":" + config.backend.port);
        metricsReporter.start();
        listener = new ProxyListener(config, logger, metrics, connectionGuard, executor);
        try {
            listener.start();
        } finally {
            stop();
        }
    }

    public void stop() {
        if (!stopped.compareAndSet(false, true)) {
            return;
        }
        if (listener != null) {
            listener.stop();
        }
        metricsReporter.stop();
        executor.shutdownNow();
        logger.info("STAShield stopped");
    }
}

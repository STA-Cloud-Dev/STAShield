package cloud.sta.stashield.network;

import cloud.sta.stashield.config.StaShieldConfig;
import cloud.sta.stashield.log.StaLogger;
import cloud.sta.stashield.metrics.MetricsRegistry;
import cloud.sta.stashield.protection.ConnectionGuard;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ProxyListener {
    private final StaShieldConfig config;
    private final StaLogger logger;
    private final MetricsRegistry metrics;
    private final ConnectionGuard connectionGuard;
    private final ExecutorService executor;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ServerSocket serverSocket;

    public ProxyListener(StaShieldConfig config,
                         StaLogger logger,
                         MetricsRegistry metrics,
                         ConnectionGuard connectionGuard,
                         ExecutorService executor) {
        this.config = config;
        this.logger = logger;
        this.metrics = metrics;
        this.connectionGuard = connectionGuard;
        this.executor = executor;
    }

    public void start() throws IOException {
        running.set(true);
        serverSocket = new ServerSocket();
        serverSocket.setReuseAddress(true);
        serverSocket.bind(new InetSocketAddress(config.server.bindHost, config.server.bindPort), config.server.backlog);
        logger.info("Listening on " + config.server.bindHost + ":" + config.server.bindPort);

        while (running.get()) {
            try {
                Socket client = serverSocket.accept();
                executor.execute(new ProxyConnectionHandler(client, config, metrics, connectionGuard, logger, executor));
            } catch (SocketException ex) {
                if (running.get()) {
                    throw ex;
                }
            }
        }
    }

    public void stop() {
        running.set(false);
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
                // Already closed.
            }
        }
    }
}

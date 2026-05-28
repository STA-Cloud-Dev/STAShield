package cloud.sta.stashield.network;

import cloud.sta.stashield.config.StaShieldConfig;
import cloud.sta.stashield.log.StaLogger;
import cloud.sta.stashield.metrics.MetricsRegistry;
import cloud.sta.stashield.protection.ConnectionGuard;
import cloud.sta.stashield.protection.GuardDecision;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;

public final class ProxyConnectionHandler implements Runnable {
    private final Socket client;
    private final StaShieldConfig config;
    private final MetricsRegistry metrics;
    private final ConnectionGuard connectionGuard;
    private final StaLogger logger;
    private final ExecutorService executor;

    public ProxyConnectionHandler(Socket client,
                                  StaShieldConfig config,
                                  MetricsRegistry metrics,
                                  ConnectionGuard connectionGuard,
                                  StaLogger logger,
                                  ExecutorService executor) {
        this.client = client;
        this.config = config;
        this.metrics = metrics;
        this.connectionGuard = connectionGuard;
        this.logger = logger;
        this.executor = executor;
    }

    @Override
    public void run() {
        String ip = remoteIp(client);
        boolean loginTracked = false;
        boolean attemptedBackend = false;
        boolean forwarded = false;
        Socket backend = null;
        metrics.recordConnectionAccepted(System.currentTimeMillis());

        try {
            if (metrics.currentConnections() > config.server.maxConnections) {
                block(ip, "max-connections", null);
                return;
            }

            GuardDecision decision = connectionGuard.evaluate(ip, System.currentTimeMillis());
            if (!decision.isAllowed()) {
                block(ip, decision.reason() + " score=" + decision.botScore()
                        + " ip_rate=" + decision.ipRate()
                        + " ip_burst=" + decision.ipBurst(), null);
                return;
            }

            client.setSoTimeout(config.server.readTimeoutMs);
            MinecraftFrame handshakeFrame = MinecraftFrame.read(client.getInputStream(), config.server.maxPacketBytes);
            HandshakeIntent intent = HandshakeIntent.parse(handshakeFrame);

            if (intent.nextState() == HandshakeIntent.NextState.STATUS) {
                metrics.recordStatusPing();
            } else if (intent.nextState() == HandshakeIntent.NextState.LOGIN) {
                GuardDecision loginDecision = connectionGuard.beginLogin(ip);
                if (!loginDecision.isAllowed()) {
                    block(ip, loginDecision.reason(), null);
                    return;
                }
                loginTracked = true;
                metrics.recordLoginConnection();
            }

            attemptedBackend = true;
            backend = connectBackend();
            OutputStream backendOutput = backend.getOutputStream();
            backendOutput.write(handshakeFrame.raw());
            backendOutput.flush();

            metrics.recordAllowed();
            forwarded = true;
            logger.info("allowed ip=" + ip
                    + " state=" + intent.nextState()
                    + " protocol=" + intent.protocolVersion()
                    + " requested_host=" + intent.serverAddress()
                    + ":" + intent.serverPort()
                    + " backend=" + config.backend.host + ":" + config.backend.port);

            ProxyPipes.bridge(client, backend, executor, logger);
        } catch (ProtocolException ex) {
            block(ip, "protocol-invalid " + ex.getMessage(), null);
        } catch (SocketTimeoutException ex) {
            block(ip, "timeout", null);
        } catch (IOException ex) {
            if (attemptedBackend && !forwarded) {
                metrics.recordBackendFailure();
                logger.warn("backend-failure ip=" + ip + " message=" + ex.getMessage());
            } else if (!forwarded) {
                block(ip, "io-before-forward " + ex.getMessage(), null);
            } else {
                logger.warn("connection-closed ip=" + ip + " message=" + ex.getMessage());
            }
        } finally {
            if (loginTracked) {
                connectionGuard.endLogin(ip);
            }
            ProxyPipes.closeQuietly(client);
            if (backend != null) {
                ProxyPipes.closeQuietly(backend);
            }
            metrics.recordConnectionClosed();
        }
    }

    private Socket connectBackend() throws IOException {
        Socket backend = new Socket();
        backend.connect(new InetSocketAddress(config.backend.host, config.backend.port), config.server.connectTimeoutMs);
        backend.setSoTimeout(config.server.readTimeoutMs);
        return backend;
    }

    private void block(String ip, String reason, Throwable throwable) {
        metrics.recordBlocked();
        if (throwable == null) {
            logger.warn("blocked ip=" + ip + " reason=" + reason);
        } else {
            logger.error("blocked ip=" + ip + " reason=" + reason, throwable);
        }
    }

    private static String remoteIp(Socket socket) {
        InetAddress address = socket.getInetAddress();
        if (address == null) {
            return "unknown";
        }
        return address.getHostAddress();
    }
}

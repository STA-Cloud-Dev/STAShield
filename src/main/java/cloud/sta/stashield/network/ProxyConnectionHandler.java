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
        long startedAt = System.currentTimeMillis();
        String ip = remoteIp(client);
        String clientPort = String.valueOf(client.getPort());
        boolean loginTracked = false;
        boolean attemptedBackend = false;
        boolean forwarded = false;
        String playerName = "unknown";
        Socket backend = null;
        metrics.recordConnectionAccepted(startedAt);

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
            MinecraftFrame loginStartFrame = null;

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

                loginStartFrame = MinecraftFrame.read(client.getInputStream(), config.server.maxPacketBytes);
                LoginStart loginStart = LoginStart.parse(loginStartFrame);
                playerName = sanitizeForLog(loginStart.username());
                logger.info("login-start player=" + playerName
                        + " ip=" + ip
                        + " client_port=" + clientPort
                        + " protocol=" + intent.protocolVersion()
                        + " requested_host=" + sanitizeForLog(intent.serverAddress()) + ":" + intent.serverPort()
                        + " backend=" + config.backend.host + ":" + config.backend.port);
            }

            attemptedBackend = true;
            backend = connectBackend();
            OutputStream backendOutput = backend.getOutputStream();
            backendOutput.write(handshakeFrame.raw());
            if (loginStartFrame != null) {
                backendOutput.write(loginStartFrame.raw());
            }
            backendOutput.flush();

            metrics.recordAllowed();
            forwarded = true;
            logger.info("allowed ip=" + ip
                    + " client_port=" + clientPort
                    + " player=" + playerName
                    + " state=" + intent.nextState()
                    + " protocol=" + intent.protocolVersion()
                    + " requested_host=" + sanitizeForLog(intent.serverAddress())
                    + ":" + intent.serverPort()
                    + " backend=" + config.backend.host + ":" + config.backend.port);

            ProxyPipes.bridge(client, backend, executor, logger);
            logger.info("disconnected ip=" + ip
                    + " client_port=" + clientPort
                    + " player=" + playerName
                    + " duration_ms=" + (System.currentTimeMillis() - startedAt));
        } catch (ProtocolException ex) {
            block(ip, "protocol-invalid " + sanitizeForLog(ex.getMessage()), null);
        } catch (SocketTimeoutException ex) {
            block(ip, "timeout", null);
        } catch (IOException ex) {
            if (attemptedBackend && !forwarded) {
                metrics.recordBackendFailure();
                logger.warn("backend-failure ip=" + ip
                        + " client_port=" + clientPort
                        + " player=" + playerName
                        + " message=" + sanitizeForLog(ex.getMessage()));
            } else if (!forwarded) {
                block(ip, "io-before-forward " + sanitizeForLog(ex.getMessage()), null);
            } else {
                logger.warn("connection-closed ip=" + ip
                        + " client_port=" + clientPort
                        + " player=" + playerName
                        + " message=" + sanitizeForLog(ex.getMessage()));
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

    private static String sanitizeForLog(String value) {
        if (value == null || value.isEmpty()) {
            return "unknown";
        }

        StringBuilder builder = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (current <= 31 || current == 127 || Character.isWhitespace(current)) {
                builder.append('_');
            } else {
                builder.append(current);
            }
        }
        return builder.toString();
    }
}

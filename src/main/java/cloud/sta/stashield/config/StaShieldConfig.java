package cloud.sta.stashield.config;

import java.util.ArrayList;
import java.util.List;

public final class StaShieldConfig {
    public final Server server = new Server();
    public final Backend backend = new Backend();
    public final Protection protection = new Protection();
    public final Access access = new Access();
    public final Metrics metrics = new Metrics();

    public static StaShieldConfig defaults() {
        return new StaShieldConfig();
    }

    public static final class Server {
        public String bindHost = "0.0.0.0";
        public int bindPort = 25565;
        public int backlog = 256;
        public int connectTimeoutMs = 5000;
        public int readTimeoutMs = 10000;
        public int maxPacketBytes = 2097152;
        public int maxConnections = 2048;
    }

    public static final class Backend {
        public String host = "127.0.0.1";
        public int port = 25566;
    }

    public static final class Protection {
        public boolean enabled = true;
        public int rateLimitPerIp = 8;
        public int burstLimitPerIp = 24;
        public long burstWindowMs = 3000L;
        public int botScoreThreshold = 70;
        public int maxActiveLoginsPerIp = 3;
    }

    public static final class Access {
        public boolean whitelistEnabled = false;
        public final List<String> whitelist = new ArrayList<String>();
        public final List<String> blacklist = new ArrayList<String>();
    }

    public static final class Metrics {
        public boolean enabled = true;
        public int logIntervalSeconds = 30;
    }
}

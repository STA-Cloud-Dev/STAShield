package cloud.sta.stashield.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TomlConfigLoaderTest {
    @TempDir
    Path tempDir;

    @Test
    void loadsTypedValuesAndIpLists() throws Exception {
        Path configPath = tempDir.resolve("stashield.toml");
        Files.write(configPath, (
                "[server]\n"
                        + "bind_host = \"127.0.0.1\"\n"
                        + "bind_port = 25570\n"
                        + "max_packet_bytes = 4096\n"
                        + "\n"
                        + "[backend]\n"
                        + "host = \"10.0.0.5\"\n"
                        + "port = 25566\n"
                        + "\n"
                        + "[protection]\n"
                        + "enabled = false\n"
                        + "rate_limit_per_ip = 12\n"
                        + "\n"
                        + "[access]\n"
                        + "whitelist_enabled = true\n"
                        + "whitelist = [\"127.0.0.1\", \"10.0.0.0/8\"]\n"
                        + "blacklist = [\"192.0.2.10\"] # comments are ignored\n"
                        + "\n"
                        + "[metrics]\n"
                        + "enabled = false\n"
                        + "log_interval_seconds = 15\n").getBytes(StandardCharsets.UTF_8));

        StaShieldConfig config = new TomlConfigLoader().load(configPath);

        assertEquals("127.0.0.1", config.server.bindHost);
        assertEquals(25570, config.server.bindPort);
        assertEquals(4096, config.server.maxPacketBytes);
        assertEquals("10.0.0.5", config.backend.host);
        assertFalse(config.protection.enabled);
        assertEquals(12, config.protection.rateLimitPerIp);
        assertTrue(config.access.whitelistEnabled);
        assertEquals(2, config.access.whitelist.size());
        assertEquals("192.0.2.10", config.access.blacklist.get(0));
        assertFalse(config.metrics.enabled);
        assertEquals(15, config.metrics.logIntervalSeconds);
    }
}

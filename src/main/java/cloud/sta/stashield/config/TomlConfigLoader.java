package cloud.sta.stashield.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class TomlConfigLoader {
    public StaShieldConfig load(Path path) throws IOException {
        StaShieldConfig config = StaShieldConfig.defaults();
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        String section = "";

        for (int index = 0; index < lines.size(); index++) {
            String raw = lines.get(index);
            int lineNumber = index + 1;
            String line = stripComment(raw).trim();
            if (line.isEmpty()) {
                continue;
            }

            if (line.startsWith("[") && line.endsWith("]")) {
                section = line.substring(1, line.length() - 1).trim().toLowerCase(Locale.ROOT);
                continue;
            }

            int equals = findUnquotedEquals(line);
            if (equals < 0) {
                throw error(path, lineNumber, "Expected key = value");
            }

            String key = line.substring(0, equals).trim().toLowerCase(Locale.ROOT);
            String value = line.substring(equals + 1).trim();
            apply(config, section, key, value, path, lineNumber);
        }

        validate(config, path);
        return config;
    }

    private void apply(StaShieldConfig config, String section, String key, String value, Path path, int lineNumber) {
        String fullKey = section + "." + key;
        if ("server.bind_host".equals(fullKey)) {
            config.server.bindHost = parseString(value, path, lineNumber);
        } else if ("server.bind_port".equals(fullKey)) {
            config.server.bindPort = parseInt(value, path, lineNumber);
        } else if ("server.backlog".equals(fullKey)) {
            config.server.backlog = parseInt(value, path, lineNumber);
        } else if ("server.connect_timeout_ms".equals(fullKey)) {
            config.server.connectTimeoutMs = parseInt(value, path, lineNumber);
        } else if ("server.read_timeout_ms".equals(fullKey)) {
            config.server.readTimeoutMs = parseInt(value, path, lineNumber);
        } else if ("server.max_packet_bytes".equals(fullKey)) {
            config.server.maxPacketBytes = parseInt(value, path, lineNumber);
        } else if ("server.max_connections".equals(fullKey)) {
            config.server.maxConnections = parseInt(value, path, lineNumber);
        } else if ("backend.host".equals(fullKey)) {
            config.backend.host = parseString(value, path, lineNumber);
        } else if ("backend.port".equals(fullKey)) {
            config.backend.port = parseInt(value, path, lineNumber);
        } else if ("protection.enabled".equals(fullKey)) {
            config.protection.enabled = parseBoolean(value, path, lineNumber);
        } else if ("protection.rate_limit_per_ip".equals(fullKey)) {
            config.protection.rateLimitPerIp = parseInt(value, path, lineNumber);
        } else if ("protection.burst_limit_per_ip".equals(fullKey)) {
            config.protection.burstLimitPerIp = parseInt(value, path, lineNumber);
        } else if ("protection.burst_window_ms".equals(fullKey)) {
            config.protection.burstWindowMs = parseLong(value, path, lineNumber);
        } else if ("protection.bot_score_threshold".equals(fullKey)) {
            config.protection.botScoreThreshold = parseInt(value, path, lineNumber);
        } else if ("protection.max_active_logins_per_ip".equals(fullKey)) {
            config.protection.maxActiveLoginsPerIp = parseInt(value, path, lineNumber);
        } else if ("access.whitelist_enabled".equals(fullKey)) {
            config.access.whitelistEnabled = parseBoolean(value, path, lineNumber);
        } else if ("access.whitelist".equals(fullKey)) {
            config.access.whitelist.clear();
            config.access.whitelist.addAll(parseStringList(value, path, lineNumber));
        } else if ("access.blacklist".equals(fullKey)) {
            config.access.blacklist.clear();
            config.access.blacklist.addAll(parseStringList(value, path, lineNumber));
        } else if ("metrics.enabled".equals(fullKey)) {
            config.metrics.enabled = parseBoolean(value, path, lineNumber);
        } else if ("metrics.log_interval_seconds".equals(fullKey)) {
            config.metrics.logIntervalSeconds = parseInt(value, path, lineNumber);
        } else {
            throw error(path, lineNumber, "Unknown config key: " + fullKey);
        }
    }

    private void validate(StaShieldConfig config, Path path) {
        requirePort(config.server.bindPort, path, "server.bind_port");
        requirePort(config.backend.port, path, "backend.port");
        requirePositive(config.server.backlog, path, "server.backlog");
        requirePositive(config.server.connectTimeoutMs, path, "server.connect_timeout_ms");
        requirePositive(config.server.readTimeoutMs, path, "server.read_timeout_ms");
        requirePositive(config.server.maxPacketBytes, path, "server.max_packet_bytes");
        requirePositive(config.server.maxConnections, path, "server.max_connections");
        requirePositive(config.protection.rateLimitPerIp, path, "protection.rate_limit_per_ip");
        requirePositive(config.protection.burstLimitPerIp, path, "protection.burst_limit_per_ip");
        requirePositive(config.protection.burstWindowMs, path, "protection.burst_window_ms");
        requirePositive(config.protection.maxActiveLoginsPerIp, path, "protection.max_active_logins_per_ip");
        requirePositive(config.metrics.logIntervalSeconds, path, "metrics.log_interval_seconds");
        if (config.protection.botScoreThreshold < 1 || config.protection.botScoreThreshold > 100) {
            throw new IllegalArgumentException(path + ": protection.bot_score_threshold must be between 1 and 100");
        }
    }

    private void requirePort(int value, Path path, String key) {
        if (value < 1 || value > 65535) {
            throw new IllegalArgumentException(path + ": " + key + " must be between 1 and 65535");
        }
    }

    private void requirePositive(int value, Path path, String key) {
        if (value < 1) {
            throw new IllegalArgumentException(path + ": " + key + " must be positive");
        }
    }

    private void requirePositive(long value, Path path, String key) {
        if (value < 1L) {
            throw new IllegalArgumentException(path + ": " + key + " must be positive");
        }
    }

    private String stripComment(String line) {
        boolean quoted = false;
        boolean escaped = false;
        for (int index = 0; index < line.length(); index++) {
            char value = line.charAt(index);
            if (escaped) {
                escaped = false;
            } else if (value == '\\') {
                escaped = true;
            } else if (value == '"') {
                quoted = !quoted;
            } else if (value == '#' && !quoted) {
                return line.substring(0, index);
            }
        }
        return line;
    }

    private int findUnquotedEquals(String line) {
        boolean quoted = false;
        boolean escaped = false;
        for (int index = 0; index < line.length(); index++) {
            char value = line.charAt(index);
            if (escaped) {
                escaped = false;
            } else if (value == '\\') {
                escaped = true;
            } else if (value == '"') {
                quoted = !quoted;
            } else if (value == '=' && !quoted) {
                return index;
            }
        }
        return -1;
    }

    private String parseString(String value, Path path, int lineNumber) {
        String trimmed = value.trim();
        if (trimmed.length() < 2 || trimmed.charAt(0) != '"' || trimmed.charAt(trimmed.length() - 1) != '"') {
            throw error(path, lineNumber, "Expected quoted string");
        }

        String content = trimmed.substring(1, trimmed.length() - 1);
        StringBuilder result = new StringBuilder(content.length());
        boolean escaped = false;
        for (int index = 0; index < content.length(); index++) {
            char current = content.charAt(index);
            if (escaped) {
                if (current == 'n') {
                    result.append('\n');
                } else if (current == 't') {
                    result.append('\t');
                } else {
                    result.append(current);
                }
                escaped = false;
            } else if (current == '\\') {
                escaped = true;
            } else {
                result.append(current);
            }
        }
        if (escaped) {
            throw error(path, lineNumber, "Dangling escape in string");
        }
        return result.toString();
    }

    private boolean parseBoolean(String value, Path path, int lineNumber) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if ("true".equals(normalized)) {
            return true;
        }
        if ("false".equals(normalized)) {
            return false;
        }
        throw error(path, lineNumber, "Expected boolean");
    }

    private int parseInt(String value, Path path, int lineNumber) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            throw error(path, lineNumber, "Expected integer");
        }
    }

    private long parseLong(String value, Path path, int lineNumber) {
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            throw error(path, lineNumber, "Expected long integer");
        }
    }

    private List<String> parseStringList(String value, Path path, int lineNumber) {
        String trimmed = value.trim();
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
            throw error(path, lineNumber, "Expected string array");
        }
        String content = trimmed.substring(1, trimmed.length() - 1).trim();
        List<String> result = new ArrayList<String>();
        if (content.isEmpty()) {
            return result;
        }

        for (String token : splitArray(content, path, lineNumber)) {
            result.add(parseString(token.trim(), path, lineNumber));
        }
        return result;
    }

    private List<String> splitArray(String content, Path path, int lineNumber) {
        List<String> tokens = new ArrayList<String>();
        boolean quoted = false;
        boolean escaped = false;
        int start = 0;
        for (int index = 0; index < content.length(); index++) {
            char value = content.charAt(index);
            if (escaped) {
                escaped = false;
            } else if (value == '\\') {
                escaped = true;
            } else if (value == '"') {
                quoted = !quoted;
            } else if (value == ',' && !quoted) {
                tokens.add(content.substring(start, index));
                start = index + 1;
            }
        }
        if (quoted) {
            throw error(path, lineNumber, "Unclosed quoted string in array");
        }
        tokens.add(content.substring(start));
        return tokens;
    }

    private IllegalArgumentException error(Path path, int lineNumber, String message) {
        return new IllegalArgumentException(path + ":" + lineNumber + ": " + message);
    }
}

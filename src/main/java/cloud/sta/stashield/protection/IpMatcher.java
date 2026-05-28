package cloud.sta.stashield.protection;

import java.util.ArrayList;
import java.util.List;

public final class IpMatcher {
    private final List<Rule> rules;

    private IpMatcher(List<Rule> rules) {
        this.rules = rules;
    }

    public static IpMatcher from(List<String> values) {
        List<Rule> parsed = new ArrayList<Rule>();
        for (String value : values) {
            String trimmed = value.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            parsed.add(parseRule(trimmed));
        }
        return new IpMatcher(parsed);
    }

    public boolean isEmpty() {
        return rules.isEmpty();
    }

    public boolean matches(String ip) {
        for (Rule rule : rules) {
            if (rule.matches(ip)) {
                return true;
            }
        }
        return false;
    }

    private static Rule parseRule(String value) {
        int slash = value.indexOf('/');
        if (slash < 0) {
            return new ExactRule(value);
        }

        String address = value.substring(0, slash);
        String prefixValue = value.substring(slash + 1);
        int prefix;
        try {
            prefix = Integer.parseInt(prefixValue);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid CIDR prefix: " + value);
        }
        if (prefix < 0 || prefix > 32) {
            throw new IllegalArgumentException("Invalid CIDR prefix: " + value);
        }
        long base = parseIpv4(address);
        long mask = prefix == 0 ? 0L : (0xffffffffL << (32 - prefix)) & 0xffffffffL;
        return new CidrRule(base & mask, mask);
    }

    private static long parseIpv4(String value) {
        String[] parts = value.split("\\.", -1);
        if (parts.length != 4) {
            throw new IllegalArgumentException("Invalid IPv4 address: " + value);
        }

        long result = 0L;
        for (String part : parts) {
            int parsed;
            try {
                parsed = Integer.parseInt(part);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Invalid IPv4 address: " + value);
            }
            if (parsed < 0 || parsed > 255) {
                throw new IllegalArgumentException("Invalid IPv4 address: " + value);
            }
            result = (result << 8) | parsed;
        }
        return result & 0xffffffffL;
    }

    private interface Rule {
        boolean matches(String ip);
    }

    private static final class ExactRule implements Rule {
        private final String ip;

        private ExactRule(String ip) {
            this.ip = ip;
        }

        @Override
        public boolean matches(String candidate) {
            return ip.equals(candidate);
        }
    }

    private static final class CidrRule implements Rule {
        private final long network;
        private final long mask;

        private CidrRule(long network, long mask) {
            this.network = network;
            this.mask = mask;
        }

        @Override
        public boolean matches(String candidate) {
            try {
                return (parseIpv4(candidate) & mask) == network;
            } catch (IllegalArgumentException ex) {
                return false;
            }
        }
    }
}

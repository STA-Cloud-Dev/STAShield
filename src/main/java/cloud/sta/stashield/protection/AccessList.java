package cloud.sta.stashield.protection;

import cloud.sta.stashield.config.StaShieldConfig;

public final class AccessList {
    private final boolean whitelistEnabled;
    private final IpMatcher whitelist;
    private final IpMatcher blacklist;

    public AccessList(StaShieldConfig.Access config) {
        this.whitelistEnabled = config.whitelistEnabled;
        this.whitelist = IpMatcher.from(config.whitelist);
        this.blacklist = IpMatcher.from(config.blacklist);
    }

    public boolean isWhitelisted(String ip) {
        return whitelist.matches(ip);
    }

    public boolean isWhitelistRequired() {
        return whitelistEnabled;
    }

    public boolean isBlacklistBlocked(String ip) {
        return blacklist.matches(ip);
    }

    public boolean hasWhitelistEntries() {
        return !whitelist.isEmpty();
    }
}

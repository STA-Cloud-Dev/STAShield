package cloud.sta.stashield.protection;

import cloud.sta.stashield.config.StaShieldConfig;

public final class BotScoreEngine {
    public int score(int ipRate, int ipBurst, int activeLogins, StaShieldConfig.Protection config) {
        int score = 0;
        score += weighted(ipRate, config.rateLimitPerIp, 40);
        score += weighted(ipBurst, config.burstLimitPerIp, 40);
        score += weighted(activeLogins, config.maxActiveLoginsPerIp, 20);
        return Math.min(100, score);
    }

    private int weighted(int observed, int limit, int weight) {
        if (observed <= 0 || limit <= 0) {
            return 0;
        }
        double ratio = (double) observed / (double) limit;
        return (int) Math.min(weight, Math.round(ratio * weight));
    }
}

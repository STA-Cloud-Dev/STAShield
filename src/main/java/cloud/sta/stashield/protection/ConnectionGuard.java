package cloud.sta.stashield.protection;

import cloud.sta.stashield.config.StaShieldConfig;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class ConnectionGuard {
    private final StaShieldConfig config;
    private final AccessList accessList;
    private final RateLimiter rateLimiter = new RateLimiter();
    private final RateLimiter burstLimiter = new RateLimiter();
    private final BotScoreEngine botScoreEngine = new BotScoreEngine();
    private final ConcurrentMap<String, AtomicInteger> activeLogins = new ConcurrentHashMap<String, AtomicInteger>();

    public ConnectionGuard(StaShieldConfig config) {
        this.config = config;
        this.accessList = new AccessList(config.access);
    }

    public GuardDecision evaluate(String ip, long nowMillis) {
        if (!config.protection.enabled) {
            return GuardDecision.allow("protection-disabled", 0, 0, 0);
        }
        if (accessList.isWhitelisted(ip)) {
            return GuardDecision.allow("whitelist", 0, 0, 0);
        }
        if (accessList.isWhitelistRequired()) {
            return GuardDecision.block("whitelist-required", 0, 0, 0);
        }
        if (accessList.isBlacklistBlocked(ip)) {
            return GuardDecision.block("blacklist", 100, 0, 0);
        }

        int ipRate = rateLimiter.record(ip, nowMillis, 1000L);
        int ipBurst = burstLimiter.record(ip, nowMillis, config.protection.burstWindowMs);
        int activeLoginCount = activeLoginCount(ip);
        int botScore = botScoreEngine.score(ipRate, ipBurst, activeLoginCount, config.protection);

        if (ipRate > config.protection.rateLimitPerIp) {
            return GuardDecision.block("rate-limit", botScore, ipRate, ipBurst);
        }
        if (ipBurst > config.protection.burstLimitPerIp) {
            return GuardDecision.block("burst-limit", botScore, ipRate, ipBurst);
        }
        if (botScore >= config.protection.botScoreThreshold) {
            return GuardDecision.block("bot-score", botScore, ipRate, ipBurst);
        }

        return GuardDecision.allow("allowed", botScore, ipRate, ipBurst);
    }

    public GuardDecision beginLogin(String ip) {
        if (!config.protection.enabled || accessList.isWhitelisted(ip)) {
            return GuardDecision.allow("login-tracking-bypass", 0, 0, 0);
        }

        AtomicInteger counter = activeLogins.get(ip);
        if (counter == null) {
            AtomicInteger created = new AtomicInteger();
            AtomicInteger previous = activeLogins.putIfAbsent(ip, created);
            counter = previous == null ? created : previous;
        }

        int active = counter.incrementAndGet();
        if (active > config.protection.maxActiveLoginsPerIp) {
            counter.decrementAndGet();
            return GuardDecision.block("active-login-limit", 100, 0, 0);
        }
        return GuardDecision.allow("login-tracked", 0, 0, 0);
    }

    public void endLogin(String ip) {
        AtomicInteger counter = activeLogins.get(ip);
        if (counter == null) {
            return;
        }
        int value = counter.decrementAndGet();
        if (value <= 0) {
            activeLogins.remove(ip, counter);
        }
    }

    private int activeLoginCount(String ip) {
        AtomicInteger value = activeLogins.get(ip);
        return value == null ? 0 : Math.max(0, value.get());
    }
}

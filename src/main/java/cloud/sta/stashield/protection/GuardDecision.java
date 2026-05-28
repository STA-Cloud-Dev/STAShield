package cloud.sta.stashield.protection;

public final class GuardDecision {
    private final boolean allowed;
    private final String reason;
    private final int botScore;
    private final int ipRate;
    private final int ipBurst;

    private GuardDecision(boolean allowed, String reason, int botScore, int ipRate, int ipBurst) {
        this.allowed = allowed;
        this.reason = reason;
        this.botScore = botScore;
        this.ipRate = ipRate;
        this.ipBurst = ipBurst;
    }

    public static GuardDecision allow(String reason, int botScore, int ipRate, int ipBurst) {
        return new GuardDecision(true, reason, botScore, ipRate, ipBurst);
    }

    public static GuardDecision block(String reason, int botScore, int ipRate, int ipBurst) {
        return new GuardDecision(false, reason, botScore, ipRate, ipBurst);
    }

    public boolean isAllowed() {
        return allowed;
    }

    public String reason() {
        return reason;
    }

    public int botScore() {
        return botScore;
    }

    public int ipRate() {
        return ipRate;
    }

    public int ipBurst() {
        return ipBurst;
    }
}

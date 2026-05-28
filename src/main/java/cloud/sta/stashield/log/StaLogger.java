package cloud.sta.stashield.log;

import java.io.PrintStream;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

public final class StaLogger {
    private final PrintStream out;
    private final PrintStream err;

    private StaLogger(PrintStream out, PrintStream err) {
        this.out = out;
        this.err = err;
    }

    public static StaLogger standard() {
        return new StaLogger(System.out, System.err);
    }

    public void info(String message) {
        log(out, "INFO", message, null);
    }

    public void warn(String message) {
        log(out, "WARN", message, null);
    }

    public void error(String message, Throwable throwable) {
        log(err, "ERROR", message, throwable);
    }

    private void log(PrintStream stream, String level, String message, Throwable throwable) {
        synchronized (stream) {
            stream.println(DateTimeFormatter.ISO_INSTANT.format(Instant.now())
                    + " [STA Cloud] [STAShield] [" + level + "] " + message);
            if (throwable != null) {
                throwable.printStackTrace(stream);
            }
        }
    }
}

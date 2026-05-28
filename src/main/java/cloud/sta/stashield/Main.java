package cloud.sta.stashield;

import cloud.sta.stashield.config.StaShieldConfig;
import cloud.sta.stashield.config.TomlConfigLoader;
import cloud.sta.stashield.log.StaLogger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) throws Exception {
        if (hasFlag(args, "--help") || hasFlag(args, "-h")) {
            printHelp();
            return;
        }
        if (hasFlag(args, "--version")) {
            System.out.println("STAShield 0.1.0");
            return;
        }

        StaLogger logger = StaLogger.standard();
        Path configPath = resolveConfigPath(args);
        StaShieldConfig config;
        if (Files.exists(configPath)) {
            config = new TomlConfigLoader().load(configPath);
            logger.info("Loaded config from " + configPath.toAbsolutePath());
        } else {
            config = StaShieldConfig.defaults();
            logger.warn("Config file not found at " + configPath.toAbsolutePath() + "; using defaults");
        }

        final StaShieldServer server = new StaShieldServer(config, logger);
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                server.stop();
            }
        }, "stashield-shutdown"));

        server.start();
    }

    private static Path resolveConfigPath(String[] args) {
        for (int index = 0; index < args.length; index++) {
            String arg = args[index];
            if ("--config".equals(arg) || "-c".equals(arg)) {
                if (index + 1 >= args.length) {
                    throw new IllegalArgumentException(arg + " requires a path");
                }
                return Paths.get(args[index + 1]);
            }
            if (arg.startsWith("--config=")) {
                return Paths.get(arg.substring("--config=".length()));
            }
        }

        Path localConfig = Paths.get("stashield.toml");
        if (Files.exists(localConfig)) {
            return localConfig;
        }
        return Paths.get("config", "stashield.example.toml");
    }

    private static boolean hasFlag(String[] args, String flag) {
        for (String arg : args) {
            if (flag.equals(arg)) {
                return true;
            }
        }
        return false;
    }

    private static void printHelp() {
        System.out.println("Usage: java -jar STAShield.jar [--config stashield.toml]");
        System.out.println("Options:");
        System.out.println("  -c, --config <path>  Load STAShield TOML config");
        System.out.println("      --version        Print version");
        System.out.println("  -h, --help           Print this help");
    }
}

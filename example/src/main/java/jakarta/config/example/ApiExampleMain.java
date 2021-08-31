package jakarta.config.example;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import java.util.logging.LogManager;

import jakarta.config.Config;
import jakarta.config.ConfigProvider;
import jakarta.config.ConfigValue;

public class ApiExampleMain {
    // for some reason (at least on my machine) the user.timezone is set to empty at startup,
    // and then suddenly changes to the correct timezone, so we get a trigger for changed configuration
    public static void main(String[] args) throws Exception {
        logConfig();
        defaults();
    }

    private static void logConfig() {
        InputStream resourceStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("logging.properties");
        try (resourceStream) {
            if (resourceStream == null) {
                System.out.println("Logging not configured, logging.properties not on classpath");
                return;
            }
            LogManager.getLogManager().readConfiguration(resourceStream);
        } catch (IOException e) {
            System.err.println("Failed to set up logging: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void defaults() throws InterruptedException {
        // get default configuration
        Config config = ConfigProvider.getConfig();

        Config serverConfig = config.get("server");
        Config portConfig = serverConfig.get("port");
        System.out.println(portConfig);

        printServerConfig(serverConfig);

        config.get("providers")
            .asNodeList()
            .ifPresent(list -> {
               list.forEach(it -> System.out.println("type: " + it.get("type").asString() + ", class: " + it.get("class").asString()));
            });

        ConfigValue configValue = portConfig.asConfigValue();
        printPortConfigValue(configValue);

        serverConfig.onChange(ApiExampleMain::printServerConfig);
        portConfig.onChange(it -> {
            System.out.println("Changed port to: " + it.asInt().orElse(-1));
            printPortConfigValue(it.asConfigValue());
        });

        // sleep so we can see changes (to see changes on classpath, modify files in target directory)
        TimeUnit.SECONDS.sleep(10);
        System.setProperty("server.port", "9999");
        TimeUnit.SECONDS.sleep(100);

    }

    private static void printPortConfigValue(ConfigValue configValue) {
        System.out.println("** Port config value **");
        System.out.println("  name:        " + configValue.getName());
        System.out.println("  value:       " + configValue.getValue());
        System.out.println("  raw value:   " + configValue.getRawValue());
        System.out.println("  source name: " + configValue.getSourceName());
        System.out.println("  source prio: " + configValue.getSourceOrdinal());
    }

    private static void printServerConfig(Config serverConfig) {
        String host = serverConfig.get("host").asString().orElse("unknown");
        int port = serverConfig.get("port").asInt().orElse(-1);
        String name = serverConfig.get("name").asString().orElse("Unknown");

        System.out.println(name + " http://" + host + ":" + port);
    }
}

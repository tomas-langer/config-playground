package jakarta.config.example;

import java.util.concurrent.TimeUnit;

import jakarta.config.Config;
import jakarta.config.ConfigProvider;

public class ApiExampleMain {
    public static void main(String[] args) throws InterruptedException {
        defaults();
    }

    private static void defaults() throws InterruptedException {
        // get default configuration
        Config config = ConfigProvider.getConfig();

        Config serverConfig = config.get("server");
        Config portConfig = serverConfig.get("port");

        printServerConfig(serverConfig);

        config.get("providers")
            .asNodeList()
            .ifPresent(list -> {
               list.forEach(it -> System.out.println("type: " + it.get("type").asString() + ", class: " + it.get("class").asString()));
            });

        serverConfig.onChange(ApiExampleMain::printServerConfig);
        portConfig.onChange(it -> System.out.println("Changed port to: " + it.asInt().orElse(-1)));
        System.out.println();

        // sleep so we can see changes
        TimeUnit.SECONDS.sleep(100);
    }

    private static void printServerConfig(Config serverConfig) {
        String host = serverConfig.get("host").asString().orElse("unknown");
        int port = serverConfig.get("port").asInt().orElse(-1);

        System.out.println("http://" + host + ":" + port);
    }
}

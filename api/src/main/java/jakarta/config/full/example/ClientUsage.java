package jakarta.config.full.example;

import java.util.Map;

import jakarta.config.full.Config;

public class ClientUsage {
    public static void main(String[] args) {
        // will use config provider
        Config config = null;

        Config serverConfig = config.get("server");
        String host = serverConfig.get("host").asString().orElse("localhost");
        int port = serverConfig.get("port").asInt().orElse(8080);

        Config detachedJsonP = config.get("json-p").detach();

        Map<String, String> theMap = detachedJsonP.asMap().orElseGet(Map::of);
        //Json.createBuilderFactory(theMap)
    }
}

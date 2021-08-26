package jakarta.config.full.example;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.config.flat.ConfigProvider;
import jakarta.config.full.Config;

public class ClientUsage {
    /*
      YAML:
      server:
        host: "localhsot"
        port: 8080
        port-count: 24

      properties:
      server.port=8081
      customer.name=sfdaf
      age=24

      json:
      {
        "server": {
            "host": "myhost"
        }
      }

      ->
      root-node (OBJECT)
        age: 24
        customer:
            name: sfdaf
        server
            host -> myhost
            port -> 8081
     */
    public static void main(String[] args) {
        // will use config provider
        Config rootNode = ConfigProvider.getConfig();

        /*
        - how to map config source to a tree structure (meta model of properties, yaml, xml, json)
        - how to represent the structure in memory (ConfigSource SPI)
        - how to represent the structure to the user (key structure, lists, objects)

        PR against MP config with tree based structure
         */

        String firstName = rootNode.getValue("list.0.name", String.class);

//        config = ConfigProvider.getConfig("server");
        rootNode.getValue("age", Integer.class);
        Config ageNode = rootNode.get("age");
        ageNode.getValue(Integer.class);

        Config serverConfig = rootNode.get("server");
        Optional<Integer> port = serverConfig.getOptionalValue("port", Integer.class);
//        Optional<Integer> port = serverConfig.get("port").asInt();
        Optional<Integer> ports = serverConfig.get("port-count").asInt();

        Config serverConfig = config.get("server");
        String host = serverConfig.get("host").asString().orElse("localhost");
        int port = serverConfig.get("port").asInt().orElse(8080);

        Config detachedJsonP = config.get("json-p").detach();

        Map<String, String> theMap = detachedJsonP.asMap().orElseGet(Map::of);
        //Json.createBuilderFactory(theMap)
        /*


         exluded=A,B
         A,B
         excluded=B
         B
         excluded=
         []
         exclude=B,C,D
         B,C,D
         */

        /*
         list=a,b,c
         list.0=d

         list:
         - a
         - b
         - c
         list: ["a", "b", "c"]

         list.0=a
         list.1=b
         list.2=c
         (0,1,2)


         list.0=3
         (3, 1, 2)

         list.0=3
         list.1=2

         (3,2,2)

         list:
            - type: "type"
              domain: "${domain-name}"

         list.0.type=type
         list.0.name=myName
         */
        Config config;
        List<Config> listNode = config.get("list")
            .getValues(Config.class)
            .get(0)
            .get("type")
            .asString();
    }
}

package jakarta.config.impl;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import jakarta.config.spi.ConfigNode;

final class ConfigUtils {
    private ConfigUtils() {
    }

    static Map<String, String> propertiesToMap(Properties properties) {
        return properties.stringPropertyNames().stream()
            .collect(Collectors.toMap(k -> k, properties::getProperty));
    }

    public static ConfigNode.ObjectNode mapToObjectNode(Map<String, String> map) {
        ObjectNodeImpl.Builder builder = ObjectNodeImpl.builder("");
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            builder.addValue(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
        }
        return builder.build();
    }

    static Map<KeyImpl, ConfigNode> createFullKeyToNodeMap(ConfigNode.ObjectNode objectNode) {
        Map<KeyImpl, ConfigNode> result;

        Stream<Map.Entry<KeyImpl, ConfigNode>> flattenNodes = objectNode.entrySet()
            .stream()
            .map(node -> flattenNodes(KeyImpl.of(node.getKey()), node.getValue()))
            .reduce(Stream.empty(), Stream::concat);
        result = flattenNodes.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        result.put(KeyImpl.of(), objectNode);

        return result;
    }

    /**
     * Create a map of keys to string values from an object node.
     *
     * @param objectNode node to flatten
     * @return a map of all nodes
     */
    public static Map<String, String> flattenNodes(ConfigNode.ObjectNode objectNode) {
        return flattenNodes(KeyImpl.of(), objectNode)
            .filter(e -> e.getValue() instanceof ValueNodeImpl)
            .collect(Collectors.toMap(
                e -> e.getKey().toString(),
                e -> Key.escapeName(((ValueNodeImpl) e.getValue()).get())
            ));
    }

    static Stream<Map.Entry<KeyImpl, ConfigNode>> flattenNodes(KeyImpl key, ConfigNode node) {
        switch (node.nodeType()) {
        case OBJECT:
            return ((ConfigNode.ObjectNode) node).entrySet().stream()
                .map(e -> flattenNodes(key.child(e.getKey()), e.getValue()))
                .reduce(Stream.of(new AbstractMap.SimpleEntry<>(key, node)), Stream::concat);
        case LIST:
            return IntStream.range(0, ((ConfigNode.ListNode) node).size())
                .boxed()
                .map(i -> flattenNodes(key.child(Integer.toString(i)), ((ConfigNode.ListNode) node).get(i)))
                .reduce(Stream.of(new AbstractMap.SimpleEntry<>(key, node)), Stream::concat);
        case VALUE:
            return Stream.of(new AbstractMap.SimpleEntry<>(key, node));
        default:
            throw new IllegalArgumentException("Invalid node type.");
        }
    }
}

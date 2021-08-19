package jakarta.config.full.example;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.config.full.spi.ConfigContent;
import jakarta.config.full.spi.ConfigNode;
import jakarta.config.full.spi.ConfigSource;
import jakarta.config.full.spi.NodeConfigSource;
import jakarta.config.full.spi.PollableConfigSource;

public class SystemPropertiesConfigSource implements ConfigSource,
                                                     NodeConfigSource,
                                                     PollableConfigSource<Map<?, ?>> {

    private final Map<?, ?> mapReference;

    public SystemPropertiesConfigSource() {
        this.mapReference = System.getProperties();
    }

    // helper methods - can be part of SPI or utility methods of implementation
    static ConfigNode.ObjectNode toNode(Map<?, ?> theMap) {
        // TODO the magic of unflattening a map to a tree
        return new ConfigNode.ObjectNode() {
            @Override
            public Optional<String> value() {
                return Optional.empty();
            }

            @Override
            public int size() {
                return 0;
            }

            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public boolean containsKey(Object key) {
                return false;
            }

            @Override
            public boolean containsValue(Object value) {
                return false;
            }

            @Override
            public ConfigNode get(Object key) {
                return null;
            }

            @Override
            public ConfigNode put(String key, ConfigNode value) {
                return null;
            }

            @Override
            public ConfigNode remove(Object key) {
                return null;
            }

            @Override
            public void putAll(Map<? extends String, ? extends ConfigNode> m) {

            }

            @Override
            public void clear() {

            }

            @Override
            public Set<String> keySet() {
                return null;
            }

            @Override
            public Collection<ConfigNode> values() {
                return null;
            }

            @Override
            public Set<Entry<String, ConfigNode>> entrySet() {
                return null;
            }
        };
    }

    @Override
    public String getName() {
        return "SystemProperties";
    }

    @Override
    public boolean isModified(Map<?, ?> stamp) {
        return !this.mapReference.equals(stamp);
    }

    @Override
    public Optional<ConfigContent.NodeContent> load() {
        return Optional.of(new NodeContentBuilder()
                               .node(toNode(mapReference))
                               // stamp is the current state of the map
                               .stamp(Map.copyOf(mapReference))
                               .build());
    }

    private static class NodeContentBuilder {
        // node based config source data
        private ConfigNode.ObjectNode rootNode;
        private Object stamp;

        /**
         * Node with the configuration of this content.
         *
         * @param rootNode the root node that links the configuration tree of this source
         * @return updated builder instance
         */
        public NodeContentBuilder node(ConfigNode.ObjectNode rootNode) {
            this.rootNode = rootNode;

            return this;
        }

        public NodeContentBuilder stamp(Object stamp) {
            this.stamp = stamp;
            return this;
        }

        ConfigNode.ObjectNode node() {
            return rootNode;
        }

        ConfigContent.NodeContent build() {
            return new ContentImpl(stamp, rootNode);
        }
    }

    private static class ContentImpl implements ConfigContent.NodeContent {

        private final Object stamp;
        private final ConfigNode.ObjectNode rootNode;

        public ContentImpl(Object stamp, ConfigNode.ObjectNode rootNode) {
            this.stamp = stamp;
            this.rootNode = rootNode;
        }

        @Override
        public Optional<Object> stamp() {
            return Optional.of(stamp);
        }

        @Override
        public ConfigNode.ObjectNode data() {
            return rootNode;
        }
    }
}

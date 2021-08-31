package jakarta.config.impl;

import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import jakarta.config.ReservedKeys;
import jakarta.config.spi.ConfigContent;
import jakarta.config.spi.ConfigNode;
import jakarta.config.spi.ConfigSource;
import jakarta.config.spi.NodeConfigSource;
import jakarta.config.spi.PollableConfigSource;

class SystemPropertiesConfigSource implements ConfigSource,
                                                     NodeConfigSource,
                                                     PollableConfigSource<Map<?, ?>> {

    private final Properties mapReference;
    private final int priority;

    SystemPropertiesConfigSource() {
        this.mapReference = System.getProperties();

        String s = mapReference.getProperty(ReservedKeys.CONFIG_PRIORITY);
        if (s == null) {
            priority = DEFAULT_PRIORITY;
        } else {
            priority = Integer.parseInt(s);
        }
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
                               .node(ConfigUtils.mapToObjectNode(ConfigUtils.propertiesToMap(mapReference)))
                               // stamp is the current state of the map
                               .stamp(Map.copyOf(mapReference))
                               .build());
    }

    @Override
    public int getPriority() {
        return priority;
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

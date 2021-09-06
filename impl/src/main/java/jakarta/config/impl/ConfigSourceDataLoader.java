package jakarta.config.impl;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.config.ReservedKeys;
import jakarta.config.spi.ConfigContent;
import jakarta.config.spi.ConfigContent.ParsableContent;
import jakarta.config.spi.ConfigNode;
import jakarta.config.spi.ConfigNode.ObjectNode;
import jakarta.config.spi.ConfigParser;
import jakarta.config.spi.ConfigSource;
import jakarta.config.spi.ConfigSourceContext;
import jakarta.config.spi.LazyConfigSource;
import jakarta.config.spi.NodeConfigSource;
import jakarta.config.spi.ParsableConfigSource;

interface ConfigSourceDataLoader {
    boolean isLazy();

    /**
     * Initial load (only called once).
     *
     * @return
     */
    Optional<ObjectNode> load();

    Optional<ObjectNode> reload();

    Object lastStamp();

    Optional<ConfigNode> get(Key key);

    int priority();

    static Optional<Integer> priority(ConfigSource configSource, Optional<ConfigNode> node) {
        return node.flatMap(ConfigNode::value)
            .map(it -> {
                try {
                    return Integer.parseInt(it);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(ReservedKeys.CONFIG_PRIORITY
                                                           + " key must be an int, but is " + it
                                                           + " in source  " + configSource.getName());
                }
            });
    }

    static ConfigSourceDataLoader create(ConfigSourceContext context, ConfigSource configSource, int defaultPriority) {
        if (configSource instanceof ParsableConfigSource) {
            return new ParsableConfigSourceLoader(context, (ParsableConfigSource) configSource, defaultPriority);
        } else if (configSource instanceof NodeConfigSource) {
            return new NodeConfigSourceLoader((NodeConfigSource) configSource, defaultPriority);
        } else if (configSource instanceof LazyConfigSource) {
            return new LazyLoader(context, (LazyConfigSource) configSource, defaultPriority);
        } else {
            throw new IllegalStateException("Config configSource " + configSource
                                                + ", class: " + configSource.getClass().getName()
                                                + ", name: " + configSource.getName()
                                                + " does not "
                                                + "implement any of required interfaces. A config configSource must at least "
                                                + "implement one of the following: ParsableSource, or NodeConfigSource, or "
                                                + "LazyConfigSource");
        }
    }

    class LazyLoader implements ConfigSourceDataLoader {
        private final LazyConfigSource configSource;
        private volatile int priority;

        private LazyLoader(ConfigSourceContext context, LazyConfigSource configSource, int defaultPriority) {
            this.configSource = configSource;
            this.priority = defaultPriority;
        }

        @Override
        public boolean isLazy() {
            return true;
        }

        @Override
        public Optional<ObjectNode> load() {
            // this is always called to get initial data, let's discover our priority
            priority = ConfigSourceDataLoader.priority(configSource, configSource.node(ReservedKeys.CONFIG_PRIORITY))
                .orElse(priority);
            return Optional.empty();
        }

        @Override
        public int priority() {
            return priority;
        }

        @Override
        public Optional<ObjectNode> reload() {
            return Optional.empty();
        }

        @Override
        public Object lastStamp() {
            return null;
        }

        @Override
        public Optional<ConfigNode> get(Key key) {
            Optional<ConfigNode> node = configSource.node(key.toString());
            node.ifPresent(it -> {
                it.configSource(configSource);
                it.sourcePriority(priority);
            });
            return node;
        }
    }

    class ParsableConfigSourceLoader extends EagerDataLoader {
        private final ParsableConfigSource configSource;
        private final ConfigSourceContext context;

        ParsableConfigSourceLoader(ConfigSourceContext context,
                                   ParsableConfigSource configSource,
                                   int defaultPriority) {
            super(configSource, defaultPriority);

            this.context = context;
            this.configSource = configSource;
        }

        @Override
        public Optional<ObjectNode> load() {
            return configSource.load()
                .map(this::stamp)
                .map(this::initialLoad)
                .map(this::registerInitialData);
        }

        @Override
        public Optional<ObjectNode> reload() {
            return configSource.load()
                .map(this::stamp)
                .map(this::load);
        }

        private ObjectNode initialLoad(ParsableContent parsableContent) {
            ObjectNode parsed = findParser(configSource, parsableContent)
                .parse(parsableContent);
            findPriority(parsed);
            return addSourceAndPriority(parsed);
        }

        private ObjectNode load(ParsableContent parsableContent) {
            return addSourceAndPriority(findParser(configSource, parsableContent)
                                            .parse(parsableContent));
        }

        private ConfigParser findParser(ParsableConfigSource configSource, ParsableContent content) {
            return configSource.parser()
                .or(() -> configSource.mediaType()
                    .or(content::mediaType)
                    .flatMap(context::findParser))
                .orElseThrow(() -> new IllegalStateException("Cannot find suitable parser for config source "
                                                                 + configSource.getName()
                                                                 + ", discovered media type: "
                                                                 + configSource.mediaType().or(content::mediaType)));
        }
    }

    class NodeConfigSourceLoader extends EagerDataLoader {
        private final NodeConfigSource configSource;

        NodeConfigSourceLoader(NodeConfigSource configSource,
                               int defaultPriority) {
            super(configSource, defaultPriority);

            this.configSource = configSource;
        }

        @Override
        public Optional<ObjectNode> load() {
            return configSource.load()
                .map(this::stamp)
                .map(ConfigContent.NodeContent::data)
                .map(this::findPriority)
                .map(this::addSourceAndPriority)
                .map(this::registerInitialData);
        }

        @Override
        public Optional<ObjectNode> reload() {
            return configSource.load()
                .map(this::stamp)
                .map(ConfigContent.NodeContent::data)
                .map(this::addSourceAndPriority);
        }
    }

    abstract class EagerDataLoader implements ConfigSourceDataLoader {
        private final AtomicReference<Object> lastStamp = new AtomicReference<>();
        private final ConfigSource configSource;

        private volatile int priority;
        private volatile ConfigNode.ObjectNode initialData;

        private EagerDataLoader(ConfigSource configSource, int defaultPriority) {
            this.configSource = configSource;
            this.priority = defaultPriority;
        }

        @Override
        public boolean isLazy() {
            return false;
        }

        @Override
        public Object lastStamp() {
            return lastStamp.get();
        }

        @Override
        public int priority() {
            return priority;
        }

        @Override
        public Optional<ConfigNode> get(Key key) {
            return objectNodeToSingleNode(key);
        }

        ObjectNode registerInitialData(ObjectNode data) {
            this.initialData = data;
            return data;
        }

        ObjectNode findPriority(ObjectNode data) {
            priority = ConfigSourceDataLoader
                .priority(configSource, Optional.ofNullable(data.get(ReservedKeys.CONFIG_PRIORITY)))
                .orElse(priority);
            return data;
        }

        ObjectNode addSourceAndPriority(ObjectNode parsed) {
            parsed.configSource(configSource);
            parsed.sourcePriority(priority);
            return parsed;
        }

        <T extends ConfigContent> T stamp(T content) {
            this.lastStamp.set(content.stamp().orElse(null));
            return content;
        }

        Optional<ConfigNode> objectNodeToSingleNode(Key key) {
            if (initialData == null) {
                throw new IllegalStateException(
                    "Single node of an eager configSource requested before load method was called."
                        + " This is a bug.");
            }

            return Optional.ofNullable(initialData.get(key.toString()));
        }
    }
}

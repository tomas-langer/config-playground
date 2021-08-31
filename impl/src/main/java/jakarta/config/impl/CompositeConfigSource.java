package jakarta.config.impl;

import java.util.Optional;

import jakarta.config.spi.ConfigContent;
import jakarta.config.spi.ConfigNode.ObjectNode;
import jakarta.config.spi.ConfigSource;
import jakarta.config.spi.ConfigSourceContext;
import jakarta.config.spi.ConfigSourceRuntime;
import jakarta.config.spi.LazyConfigSource;
import jakarta.config.spi.NodeConfigSource;

class CompositeConfigSource implements ConfigSource, NodeConfigSource {
    private final String name;
    private final ConfigSource main;
    private final ConfigSource fallback;
    private ConfigSourceRuntime mainRuntime;
    private ConfigSourceRuntime fallbackRuntime;

    CompositeConfigSource(String name, ConfigSource main, ConfigSource fallback) {
        this.name = name;
        this.main = main;
        this.fallback = fallback;

        isGoodType("Main", main);
        isGoodType("Fallback", fallback);
    }

    private static void isGoodType(String name, ConfigSource source) {
        if (source instanceof LazyConfigSource) {
            throw new IllegalArgumentException("Composite config configSource's "
                                                   + name
                                                   + " cannot be a lazy config configSource. Use "
                                                   + "either Parsable or Node config sources");
        }
    }

    @Override
    public void init(ConfigSourceContext context) {
        this.mainRuntime = context.sourceRuntime(main);
        this.fallbackRuntime = context.sourceRuntime(fallback);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Optional<ConfigContent.NodeContent> load() {
        Optional<ObjectNode> mainLoad = mainRuntime.load();
        Optional<ObjectNode> fallbackLoad = fallbackRuntime.load();

        if (mainLoad.isPresent() && fallbackLoad.isPresent()) {
            ObjectNode mainNode = mainLoad.get();
            ObjectNode fallbackNode = fallbackLoad.get();

            return Optional.of(new NodeContentImpl(fallbackNode.merge(mainNode)));
        }

        return mainLoad.or(() -> fallbackLoad)
            .map(NodeContentImpl::new);
    }
}

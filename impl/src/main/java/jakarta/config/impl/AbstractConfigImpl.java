package jakarta.config.impl;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import jakarta.config.Config;
import jakarta.config.spi.ConfigSource;
import jakarta.config.spi.Converter;

abstract class AbstractConfigImpl implements Config {
    private final ConfigFactory factory;
    private final ConversionSupport conversionSupport;

    private final KeyImpl keyPrefix;
    private final KeyImpl key;
    private final KeyImpl realKey;
    private final Config.Type type;
    private final String name;
    private final Context context;

    AbstractConfigImpl(Type type,
                       KeyImpl prefix,
                       KeyImpl key,
                       ConfigFactory factory,
                       ConversionSupport conversionSupport) {
        this.type = type;
        this.keyPrefix = prefix;
        this.key = key;
        this.factory = factory;
        this.conversionSupport = conversionSupport;

        this.realKey = keyPrefix.child(key);
        this.name = key.name();
        this.context = new NodeContextImpl();
    }

    @Override
    public Type type() {
        return type;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public <T> T unwrap(Class<T> type) {
        throw new IllegalArgumentException("Config cannot be unwrapped to a publicly available type");
    }

    @Override
    public String key() {
        return key.toString();
    }

    @Override
    public final Config get(String key) {
        Objects.requireNonNull(key, "Key argument is null.");

        if (key.isEmpty()) {
            return this;
        } else {
            return factory.config(this.keyPrefix, this.key.child(key));
        }
    }

    @Override
    public Optional<List<Config>> asNodeList() {
        return asList(Config.class);
    }

    @Override
    public final Config detach() {
        if (key.isRoot()) {
            return this;
        } else {
            return factory.config(realKey(), KeyImpl.of());
        }
    }

    @Override
    public <T> Optional<Converter<T>> getConverter(Class<T> forType) {
        return conversionSupport.converter(forType);
    }

    @Override
    public Iterable<ConfigSource> getConfigSources() {
        return factory.provider().configSources();
    }

    @Override
    public void onChange(Consumer<Config> onChangeConsumer) {
        factory.provider()
            .onChange(event -> {
                // check if change contains this node
                if (event.changedKeys().contains(realKey)) {
                    onChangeConsumer.accept(contextConfig(event.config()));
                }
            });
    }

    @Override
    public Context context() {
        return context;
    }

    protected final KeyImpl realKey() {
        return realKey;
    }

    /**
     * Returns a {@code String} value as {@link java.util.Optional} of configuration node if the node a leaf or "hybrid" node.
     * Returns a {@link java.util.Optional#empty() empty} if the node is {@link Type#MISSING} type or if the node does not
     * contain a direct
     * value.
     * This is "raw" accessor method for String value of this config node. To have nicer variety of value accessors,
     * see {@link #asString()} and in general {@link #as(Class)}.
     *
     * @return value as type instance as {@link java.util.Optional}, {@link java.util.Optional#empty() empty} in case the node
     * does not have a value
     *
     * use {@link #asString()} instead
     */
    Optional<String> value() {
        return Optional.empty();
    }

    private Config contextConfig(Config rootConfig) {
        return rootConfig
            .get(AbstractConfigImpl.this.keyPrefix.toString())
            .detach()
            .get(AbstractConfigImpl.this.key.toString());
    }

    /**
     * Implementation of node specific context.
     */
    private class NodeContextImpl implements Context {

        @Override
        public Instant timestamp() {
            return AbstractConfigImpl.this.factory.context().timestamp();
        }

        @Override
        public Config last() {
            return AbstractConfigImpl.this.contextConfig(AbstractConfigImpl.this.factory.context().last());
        }

        @Override
        public Config reload() {
            return AbstractConfigImpl.this.contextConfig(AbstractConfigImpl.this.factory.context().reload());
        }
    }
}

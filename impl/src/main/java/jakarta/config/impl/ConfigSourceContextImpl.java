package jakarta.config.impl;

import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;

import jakarta.config.spi.ConfigSourceContext;
import jakarta.config.spi.ConfigParser;
import jakarta.config.spi.ConfigSource;
import jakarta.config.spi.ConfigSourceRuntime;

class ConfigSourceContextImpl implements ConfigSourceContext {
    private final Map<ConfigSource, ConfigSourceRuntimeImpl> runtimes = new IdentityHashMap<>();

    private final Executor changesExecutor;
    private final List<ConfigParser> configParsers;

    ConfigSourceContextImpl(Executor changesExecutor, List<ConfigParser> configParsers) {
        this.changesExecutor = changesExecutor;
        this.configParsers = configParsers;
    }

    @Override
    public ConfigSourceRuntime sourceRuntime(ConfigSource source) {
        return runtimes.computeIfAbsent(source,
                                        it -> new ConfigSourceRuntimeImpl(this,
                                                                          source,
                                                                          Priorities.find(source,
                                                                                          ConfigSource.DEFAULT_PRIORITY)));
    }

    ConfigSourceRuntimeImpl sourceRuntime(ConfigSource source, int priority) {
        return runtimes.computeIfAbsent(source,
                                        it -> new ConfigSourceRuntimeImpl(this,
                                                                          source,
                                                                          priority));
    }

    Optional<ConfigParser> findParser(String mediaType) {
        Objects.requireNonNull(mediaType, "Unknown media type of resource.");

        return configParsers.stream()
            .filter(parser -> parser.supportedMediaTypes().contains(mediaType))
            .findFirst();
    }

    Executor changesExecutor() {
        return changesExecutor;
    }

    List<String> supportedSuffixes() {
        List<String> result = new LinkedList<>();

        configParsers.stream()
            .map(ConfigParser::supportedSuffixes)
            .forEach(result::addAll);

        return result;
    }
}

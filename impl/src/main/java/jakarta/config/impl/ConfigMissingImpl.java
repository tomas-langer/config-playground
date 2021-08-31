package jakarta.config.impl;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import jakarta.config.Config;
import jakarta.config.ConfigValue;

class ConfigMissingImpl extends AbstractConfigImpl {
    ConfigMissingImpl(KeyImpl prefix,
                      KeyImpl key,
                      ConfigFactory factory,
                      ConversionSupport mapperManager) {
        super(Type.MISSING, prefix, key, factory, mapperManager);
    }

    @Override
    public boolean hasValue() {
        return false;
    }

    @Override
    public <T> Optional<T> as(Class<T> type) {
        return Optional.empty();
    }

    @Override
    public <T> Optional<List<T>> asList(Class<T> type) {
        return Optional.empty();
    }

    @Override
    public Optional<Map<String, String>> asMap() {
        return Optional.empty();
    }

    @Override
    public Stream<Config> traverse(Predicate<Config> predicate) {
        return Stream.empty();
    }

    @Override
    public <T> Optional<T> as(Function<Config, T> converter) {
        return Optional.empty();
    }

    @Override
    public Optional<String> asString() {
        return Optional.empty();
    }

    @Override
    public ConfigValue asConfigValue() {
        return new ConfigValue() {
            @Override
            public String getName() {
                return key();
            }

            @Override
            public String getValue() {
                return null;
            }

            @Override
            public String getRawValue() {
                return null;
            }

            @Override
            public String getSourceName() {
                return null;
            }

            @Override
            public int getSourceOrdinal() {
                return 0;
            }
        };
    }

    @Override
    public String toString() {
        return "[" + realKey() + "] MISSING";
    }
}

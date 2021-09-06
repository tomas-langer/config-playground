package jakarta.config.impl;

import jakarta.common.Priorities;
import jakarta.config.spi.ConfigSource;

class OrdinalConfigSource {
    static final int BUILT_IN_PRIORITY = 10000;

    private final int priority;
    private final ConfigSource configSource;

    OrdinalConfigSource(ConfigSource configSource) {
        this(configSource, Priorities.find(configSource, ConfigSource.DEFAULT_PRIORITY));
    }

    OrdinalConfigSource(ConfigSource configSource, int priority) {
        this.priority = priority;
        this.configSource = configSource;
    }

    int priority() {
        return priority;
    }

    ConfigSource configSource() {
        return configSource;
    }

    @Override
    public String toString() {
        return priority + ": " + configSource.getName();
    }
}

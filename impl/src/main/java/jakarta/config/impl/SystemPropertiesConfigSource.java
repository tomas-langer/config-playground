package jakarta.config.impl;

import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import jakarta.config.ReservedKeys;
import jakarta.config.spi.ConfigContent;
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
        Map<Object, Object> stamp = Map.copyOf(mapReference);
        return Optional.of(new NodeContentImpl(ConfigUtils.mapToObjectNode(ConfigUtils.propertiesToMap(mapReference)),
                                               stamp));
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public String toString() {
        return getName();
    }
}

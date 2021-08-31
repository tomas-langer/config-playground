package jakarta.config.impl;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import jakarta.config.ReservedKeys;
import jakarta.config.spi.ConfigNode;
import jakarta.config.spi.ConfigSource;
import jakarta.config.spi.LazyConfigSource;

class EnvironmentVariablesConfigSource implements ConfigSource,
                                                  LazyConfigSource {

    private static final Pattern DISALLOWED_CHARS = Pattern.compile("[^a-zA-Z0-9_]");
    private static final String UNDERSCORE = "_";

    private final Map<String, String> env;
    private final Map<String, Cached> cache = new ConcurrentHashMap<>();
    private final int priority;

    EnvironmentVariablesConfigSource() {
        this.env = System.getenv();
        String s = env.get(ReservedKeys.CONFIG_PRIORITY);
        if (s == null) {
            priority = DEFAULT_PRIORITY;
        } else {
            priority = Integer.parseInt(s);
        }
    }

    /**
     * Rule #2 states: Replace each character that is neither alphanumeric nor _ with _ (i.e. com_ACME_size).
     *
     * @param propertyName name of property as requested by user
     * @return name of environment variable we look for
     */
    private static String rule2(String propertyName) {
        return DISALLOWED_CHARS.matcher(propertyName).replaceAll(UNDERSCORE);
    }

    @Override
    public Optional<ConfigNode> node(String key) {
        // environment variable config configSource is immutable - we can safely cache all requested keys, so we
        // do not execute the regular expression on every get
        return cache.computeIfAbsent(key, theKey -> {
                // According to the spec, we have three ways of looking for a property
                // 1. Exact match
                String result = env.get(key);
                if (null != result) {
                    return new Cached(result);
                }
                // 2. replace non alphanumeric characters with _
                String rule2 = rule2(key);
                result = env.get(rule2);
                if (null != result) {
                    return new Cached(result);
                }
                // 3. replace same as above, but uppercase
                String rule3 = rule2.toUpperCase();
                result = env.get(rule3);
                return new Cached(result);
            }).value()
            .map(it -> createValueNode(key, it));
    }

    private ConfigNode.ValueNode createValueNode(String key, String value) {
        return ValueNodeImpl.create(key, value);
    }

    @Override
    public String getName() {
        return "Environment Variables";
    }

    @Override
    public String toString() {
        return getName() + "(" + getPriority() + ")";
    }

    @Override
    public int getPriority() {
        return priority;
    }

    private static final class Cached {
        // optional field, as the only accessor requires it - no need to create a new instance for every call.
        private final Optional<String> value;

        private Cached(String value) {
            this.value = Optional.ofNullable(value);
        }

        private Optional<String> value() {
            return value;
        }
    }
}

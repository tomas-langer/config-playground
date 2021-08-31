package jakarta.config.spi;

import java.util.Optional;

public interface LazyConfigSource extends ConfigSource {
    /**
     * Provide a value for the node on the requested key.
     *
     * @param key key to look for
     * @return value of the node if available in the source
     */
    Optional<ConfigNode> node(String key);
}

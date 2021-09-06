package jakarta.config.impl;

import java.util.Properties;
import java.util.Set;

import jakarta.common.Prioritized;
import jakarta.config.spi.ConfigContent;
import jakarta.config.spi.ConfigNode;
import jakarta.config.spi.ConfigParser;

public class PropertiesParser implements ConfigParser, Prioritized {
    /**
     * A String constant representing {@value} media type.
     */
    public static final String MEDIA_TYPE_TEXT_JAVA_PROPERTIES = "text/x-java-properties";
    /**
     * Priority of the parser used if registered from service loader.
     */
    public static final int PRIORITY = ConfigParser.PRIORITY + 100;

    private static final Set<String> SUPPORTED_MEDIA_TYPES = Set.of(MEDIA_TYPE_TEXT_JAVA_PROPERTIES);

    @Override
    public Set<String> supportedMediaTypes() {
        return SUPPORTED_MEDIA_TYPES;
    }

    @Override
    public ConfigNode.ObjectNode parse(ConfigContent.ParsableContent content) {
        Properties properties = new Properties();
        try {
            properties.load(content.data());
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot read properties from configSource: " + e.getLocalizedMessage(), e);
        }

        return ConfigUtils.mapToObjectNode(ConfigUtils.propertiesToMap(properties));
    }

    @Override
    public Set<String> supportedSuffixes() {
        return Set.of("properties");
    }

    @Override
    public int priority() {
        return PRIORITY;
    }
}

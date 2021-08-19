package jakarta.config.full.example;

import java.util.Properties;
import java.util.Set;

import jakarta.config.full.spi.ConfigContent;
import jakarta.config.full.spi.ConfigNode;
import jakarta.config.full.spi.ConfigParser;

public class PropertiesConfigParser implements ConfigParser {
    /**
     * A String constant representing {@value} media type.
     */
    public static final String MEDIA_TYPE_TEXT_JAVA_PROPERTIES = "text/x-java-properties";

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
            throw new IllegalStateException("Cannot read properties from source: " + e.getLocalizedMessage(), e);
        }
        return SystemPropertiesConfigSource.toNode(properties);
    }
}

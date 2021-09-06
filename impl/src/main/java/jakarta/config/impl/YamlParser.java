package jakarta.config.impl;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.common.Prioritized;
import jakarta.config.spi.ConfigContent;
import jakarta.config.spi.ConfigNode.ListNode;
import jakarta.config.spi.ConfigNode.ObjectNode;
import jakarta.config.spi.ConfigParser;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

@SuppressWarnings("rawtypes")
public class YamlParser implements ConfigParser, Prioritized {
    /**
     * A String constant representing {@value} media type.
     */
    public static final String MEDIA_TYPE_APPLICATION_YAML = "application/x-yaml";
    /**
     * Priority of the parser used if registered from service loader.
     */
    public static final int PRIORITY = ConfigParser.PRIORITY + 100;

    private static final Set<String> SUPPORTED_MEDIA_TYPES = Set.of(MEDIA_TYPE_APPLICATION_YAML);
    private static final Set<String> SUPPORTED_SUFFIXES = Set.of("yml", "yaml");

    @Override
    public Set<String> supportedMediaTypes() {
        return SUPPORTED_MEDIA_TYPES;
    }

    @Override
    public ObjectNode parse(ConfigContent.ParsableContent content) {
        try (InputStreamReader reader = new InputStreamReader(content.data(), content.charset())) {
            Map yamlMap = toMap(reader);
            if (yamlMap == null) { // empty configSource
                return ObjectNodeImpl.builder("").build();
            }

            return fromMap(yamlMap);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot read from configSource: " + e.getLocalizedMessage(), e);
        }
    }

    @Override
    public Set<String> supportedSuffixes() {
        return SUPPORTED_SUFFIXES;
    }

    @Override
    public int priority() {
        return PRIORITY;
    }

    Map toMap(Reader reader) {
        // the default of Snake YAML is a Map, safe constructor makes sure we never deserialize into anything
        // harmful
        Yaml yaml = new Yaml(new SafeConstructor());
        return (Map) yaml.loadAs(reader, Object.class);
    }

    private ObjectNode fromMap(Map<?, ?> map) {
        ObjectNodeImpl.Builder builder = ObjectNodeImpl.builder("");
        if (map != null) {
            map.forEach((k, v) -> {
                String strKey = k.toString();
                if (v instanceof List) {
                    builder.addList(strKey, fromList(strKey, (List) v));
                } else if (v instanceof Map) {
                    builder.addObject(strKey, fromMap((Map) v));
                } else {
                    String strValue = v == null ? "" : v.toString();
                    builder.addValue(strKey, strValue);
                }
            });
        }
        return builder.build();
    }

    private ListNode fromList(String key, List<?> list) {
        ListNodeImpl.Builder builder = ListNodeImpl.builder(key);
        list.forEach(value -> {
            if (value instanceof List) {
                builder.addList(fromList(key, (List) value));
            } else if (value instanceof Map) {
                builder.addObject(fromMap((Map) value));
            } else {
                String strValue = value == null ? "" : value.toString();
                builder.addValue(ValueNodeImpl.create(key, strValue));
            }
        });
        return builder.build();
    }
}

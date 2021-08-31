package jakarta.config.impl;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import jakarta.config.Config;
import jakarta.config.spi.ConfigNode.ValueNode;

public class ConfigLeafImpl extends ConfigExistingImpl<ValueNode> {
    private static final Pattern SPLIT_PATTERN = Pattern.compile("(?<!\\\\),");
    private static final Pattern ESCAPED_COMMA_PATTERN = Pattern.compile("\\,", Pattern.LITERAL);

    private final ConversionSupport mapperManager;

    ConfigLeafImpl(KeyImpl prefix,
                   KeyImpl key,
                   ValueNode valueNode,
                   ConfigFactory factory,
                   ConversionSupport mapperManager) {
        super(Type.VALUE, prefix, key, valueNode, factory, mapperManager);
        this.mapperManager = mapperManager;
    }

    static String[] toArray(String stringValue) {
        String[] values = SPLIT_PATTERN.split(stringValue, -1);

        for (int i = 0; i < values.length; i++) {
            String value = values[i];
            values[i] = ESCAPED_COMMA_PATTERN.matcher(value).replaceAll(Matcher.quoteReplacement(","));
        }
        return values;
    }

    @Override
    public <T> Optional<List<T>> asList(Class<T> type) {
        if (Config.class.equals(type)) {
            throw new IllegalArgumentException(key() + ": the Config node represents single value, cannot be represented as a "
                                                   + "list of nodes");
        }

        Optional<String> value = value();
        if (value.isEmpty()) {
            return Optional.empty();
        }

        String stringValue = value.get();
        if (stringValue.contains(",")) {
            // the value may be a comma separated string, with optional escape of commas with
            // backslash
            String[] parts = toArray(stringValue);
            List<T> result = new LinkedList<>();
            for (String part : parts) {
                result.add(mapperManager.convert(part, type, key()));
            }
            return Optional.of(result);
        } else {
            return Optional.of(List.of(mapperManager.convert(stringValue, type, name())));
        }
    }

    @Override
    public Stream<Config> traverse(Predicate<Config> predicate) {
        return Stream.empty();
    }

    @Override
    public String toString() {
        return "[" + realKey() + "] VALUE '" + node().get() + "'";
    }

}

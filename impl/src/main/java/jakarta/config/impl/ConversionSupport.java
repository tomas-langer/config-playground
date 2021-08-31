package jakarta.config.impl;

import java.util.Map;
import java.util.Optional;

import jakarta.config.Config;
import jakarta.config.spi.Converter;

class ConversionSupport {
    private final Map<Class<?>, Converter<?>> converters;

    ConversionSupport(Map<Class<?>, Converter<?>> converters) {
        this.converters = converters;
    }

    @SuppressWarnings("unchecked")
    <T> Optional<Converter<T>> converter(Class<T> type) {
        return Optional.ofNullable((Converter<T>) converters.get(type));
    }

    <T> T convert(String value, Class<T> type, String key) {
        return converter(type)
            .map(converter -> {
                if (converter.useConfigNode()) {
                    KeyImpl keyImpl = KeyImpl.of(key);
                    return converter.convert(new ConfigLeafImpl(keyImpl.parent(),
                                                                KeyImpl.of(keyImpl.name()),
                                                                ValueNodeImpl.create(key, value),
                                                                null,
                                                                this));
                } else {
                    return converter.convert(value);
                }
            })
            .orElseThrow(() -> new IllegalArgumentException("Cannot convert key " + key + " to " + type.getName() + ", missing "
                                                                + "converter"));
    }

    <T> T convert(Config node, Class<T> type, String key) {
        /*
         * String is the "native" format
         */
        if (type.equals(String.class)) {
            return (T) node.asString().get();
        }

        return converter(type)
            .map(converter -> {
                if (converter.useConfigNode()) {
                    return converter.convert(node);
                } else {
                    if (node.hasValue()) {
                        return converter.convert(node.asString().get());
                    } else {
                        return null;
                    }
                }
            })
            .orElseThrow(() -> new IllegalArgumentException("Cannot convert key " + key + " to " + type.getName() + ", missing "
                                                                + "converter"));
    }
}

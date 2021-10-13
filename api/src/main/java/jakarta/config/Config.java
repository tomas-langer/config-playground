/*
 * Copyright (c) 2021 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jakarta.config;

import java.util.Optional;
import java.util.function.Function;

import jakarta.config.spi.ConfigSource;
import jakarta.config.spi.Converter;
import jakarta.config.spi.StringConverter;

/**
 * Access to configuration values.
 */
public interface Config {
    /**
     * Return all the currently registered {@link jakarta.config.spi.ConfigSource sources} for this configuration.
     * <p>
     * The returned sources will be sorted by priority and name, which can be iterated in a thread-safe
     * manner. The {@link java.lang.Iterable Iterable} contains a fixed number of {@link jakarta.config.spi.ConfigSource
     * configuration
     * sources}, determined at application start time, and the config sources themselves may be static or dynamic.
     *
     * @return the configuration sources
     */
    Iterable<ConfigSource> getConfigSources();

    /**
     * Return the {@link jakarta.config.spi.Converter} used by this instance to produce instances of the specified type from
     * string values.
     *
     * @param <T>
     *            the conversion type
     * @param forType
     *            the type to be produced by the converter
     * @return an {@link java.util.Optional} containing the converter, or empty if no converter is available for the specified
     *         type
     */
    <T> Optional<Converter<T>> getConverter(Class<T> forType);

    /**
     * Return the {@link jakarta.config.spi.StringConverter} used by this instance to produce instances of the specified type from
     * string values.
     * This is a helper method to provide simple conversions (such as from a default value of an annotation).
     *
     * @param <T>
     *            the conversion type
     * @param forType
     *            the type to be produced by the converter
     * @return an {@link java.util.Optional} containing the converter, or empty if no converter is available for the specified
     *         type
     */
    <T> Optional<StringConverter<T>> getStringConverter(Class<T> forType);

    /**
     * Returns an instance of the specific class, to allow access to the provider specific API.
     * <p>
     * If the MP Config provider implementation does not support the specified class, a {@link IllegalArgumentException}
     * is thrown.
     * <p>
     * Unwrapping to the provider specific API may lead to non-portable behaviour.
     *
     * @param type
     *            Class representing the type to unwrap to
     * @param <T>
     *            The type to unwrap to
     * @return An instance of the given type
     * @throws IllegalArgumentException
     *             If the current provider does not support unwrapping to the given type
     */
    <T> T unwrap(Class<T> type);

    /**
     * Fully qualified key of this config node (such as {@code server.port}).
     * Returns an empty String for root config.
     *
     * @return key of this config
     */
    String key();

    /**
     * Name of this node - the last element of a fully qualified key.
     * <p>
     * For example for key {@code server.port} this method would return {@code port}.
     *
     * @return name of this node
     */
    String name();

    /**
     * Single sub-node for the specified sub-key.
     * For example if requested for key {@code server}, this method would return a config
     * representing the {@code server} node, which would have for example a child {@code port}.
     * The sub-key can return more than one level of nesting (e.g. using {@code server.tls} would
     * return a node that contains the TLS configuration under {@code server} node).
     *
     * @param key sub-key to retrieve nested node.
     * @return sub node, never null
     */
    Config get(String key);

    /**
     * Typed value created using a converter function.
     * The converter is called only if this config node exists.
     *
     * @param converter to create an instance from config node
     * @param <T> type of the object
     * @return converted value of this node, or an empty optional if this node does not exist
     * @throws java.lang.IllegalArgumentException if this config node cannot be converted to the desired type
     */
    <T> Optional<T> as(Function<Config, T> converter);

    /**
     * Typed value created using a discovered/built-in converter.
     *
     * @param type class to convert to
     * @param <T> type of the object
     * @return converted value of this node, or an empty optional if this node does not exist
     * @throws java.lang.IllegalArgumentException if this config node cannot be converted to the desired type
     */
    <T> Optional<T> as(Class<T> type);

    /**
     * Direct value of this node used for string converters.
     *
     * @return value of this node
     */
    Optional<String> asString();

    /**
     * Config value associated with this tree node (if any).
     * If this node represents a value obtained from a config source, this method must return a non-empty value.
     *
     * @return config value of this node, or empty if this node does not represent a direct value
     */
    Optional<ConfigValue> configValue();

    /*
     * Shortcut helper methods
     */
    default Optional<Integer> asInt() {
        return as(Integer.class);
    }
}

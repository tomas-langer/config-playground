/*
 * Copyright (c) 2017, 2020 Oracle and/or its affiliates.
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

package jakarta.config.impl;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import jakarta.config.Config;
import jakarta.config.ConfigValue;
import jakarta.config.spi.ConfigNode;
import jakarta.config.spi.ConfigSource;

/**
 * Implementation of {@link Config} that represents NOT {@link Type#MISSING missing} node.
 *
 * @param <N> type of node
 */
abstract class ConfigExistingImpl<N extends ConfigNode> extends AbstractConfigImpl {

    private final N node;
    private final ConversionSupport mapperManager;

    ConfigExistingImpl(Type type,
                       KeyImpl prefix,
                       KeyImpl key,
                       N node,
                       ConfigFactory factory,
                       ConversionSupport mapperManager) {
        super(type, prefix, key, factory, mapperManager);

        Objects.requireNonNull(node, "node argument is null.");
        Objects.requireNonNull(mapperManager, "mapperManager argument is null.");

        this.node = node;
        this.mapperManager = mapperManager;
    }

    @Override
    public final Optional<String> value() {
        return node.value();
    }

    @Override
    public boolean hasValue() {
        return node().value().isPresent();
    }

    @Override
    public <T> Optional<T> as(Class<T> type) {
        if (type.equals(String.class)) {
            return (Optional<T>) asString();
        }
        return Optional.of(mapperManager.convert(this, type, key()));
    }

    @Override
    public Optional<String> asString() {
        return value();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Optional<Map<String, String>> asMap() {
        return Optional.of(mapperManager.convert(this, Map.class, key()));
    }

    @Override
    public <T> Optional<T> as(Function<Config, T> converter) {
        return Optional.of(converter.apply(this));
    }

    @Override
    public ConfigValue asConfigValue() {
        return new ConfigValue() {
            @Override
            public String getName() {
                return key();
            }

            @Override
            public String getValue() {
                return value().orElse(null);
            }

            @Override
            public String getRawValue() {
                return value().orElse(null);
            }

            @Override
            public String getSourceName() {
                return node.configSource()
                    .map(ConfigSource::getName)
                    .orElse(null);
            }

            @Override
            public int getSourceOrdinal() {
                return node.sourcePriority().orElse(0);
            }
        };
    }

    protected final N node() {
        return node;
    }
}

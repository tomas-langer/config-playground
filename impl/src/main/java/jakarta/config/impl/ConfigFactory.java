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

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import jakarta.config.Config;
import jakarta.config.spi.ConfigNode;
import jakarta.config.spi.ConfigSource;

/**
 * The factory class creates and caches already created instances of {@link Config} for specified {@code key}.
 */
final class ConfigFactory {

    private final ConversionSupport mapperManager;
    private final Map<KeyImpl, ConfigNode> fullKeyToNodeMap;
    private final ProviderImpl provider;
    private final ConcurrentMap<PrefixedKey, AbstractConfigImpl> configCache;
    private final Instant timestamp;

    /**
     * Create new instance of the factory operating on specified {@link ConfigSource}.
     * @param mapperManager  manager to be used to map string value to appropriate type
     * @param node           root configuration node provided by the configuration source to be used to build
     *                       {@link Config} instances on.
     * @param provider       shared config provider
     */
    ConfigFactory(ConversionSupport mapperManager,
                  ConfigNode.ObjectNode node,
                  ProviderImpl provider) {

        Objects.requireNonNull(mapperManager, "mapperManager argument is null.");
        Objects.requireNonNull(node, "node argument is null.");
        Objects.requireNonNull(provider, "provider argument is null.");

        this.mapperManager = mapperManager;
        this.fullKeyToNodeMap = ConfigUtils.createFullKeyToNodeMap(node);
        this.provider = provider;

        configCache = new ConcurrentHashMap<>();
        timestamp = Instant.now();
    }

    Instant timestamp() {
        return timestamp;
    }

    /**
     * Get existing or create new root {@link Config} instance.
     *
     * @return root {@link Config}
     */
    AbstractConfigImpl config() {
        return config(KeyImpl.of(), KeyImpl.of());
    }

    /**
     * Get existing or create new {@link Config} instance for specified {@code key}.
     *
     * @param prefix key prefix (to support detached roots)
     * @param key    config key
     * @return {@code key} specific instance of {@link Config}
     */
    AbstractConfigImpl config(KeyImpl prefix, KeyImpl key) {
        PrefixedKey prefixedKey = new PrefixedKey(prefix, key);

        return configCache.computeIfAbsent(prefixedKey, it -> createConfig(prefix, key));
    }

    /**
     * Create new instance of {@link Config}.
     *
     * @param key config key
     * @return new instance of {@link Config} for specified {@code key}
     */
    private AbstractConfigImpl createConfig(KeyImpl prefix, KeyImpl key) {
        ConfigNode value = findNode(prefix, key);

        if (null == value) {
            return new ConfigMissingImpl(prefix, key, this, mapperManager);
        }

        switch (value.nodeType()) {
        case OBJECT:
            return new ConfigObjectImpl(prefix, key, (ConfigNode.ObjectNode) value, this, mapperManager);
        case LIST:
            return new ConfigListImpl(prefix, key, (ConfigNode.ListNode) value, this, mapperManager);
        case VALUE:
            return new ConfigLeafImpl(prefix, key, (ConfigNode.ValueNode) value, this, mapperManager);
        default:
            return new ConfigMissingImpl(prefix, key, this, mapperManager);
        }
    }

    private ConfigNode findNode(KeyImpl prefix, KeyImpl key) {
        return fullKeyToNodeMap.get(prefix.child(key));
    }

    /**
     * Returns whole configuration context.
     *
     * @return whole configuration context.
     */
    Config.Context context() {
        return provider;
    }

    ProviderImpl provider() {
        return provider;
    }

    /**
     * Prefix represents detached roots.
     */
    private static final class PrefixedKey {
        private final KeyImpl prefix;
        private final KeyImpl key;

        private PrefixedKey(KeyImpl prefix, KeyImpl key) {
            this.prefix = prefix;
            this.key = key;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            PrefixedKey that = (PrefixedKey) o;
            return Objects.equals(prefix, that.prefix)
                    && Objects.equals(key, that.key);
        }

        @Override
        public int hashCode() {
            return Objects.hash(prefix, key);
        }
    }

}

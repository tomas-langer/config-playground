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
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.config.Config;
import jakarta.config.spi.ConfigNode.ObjectNode;
import jakarta.config.spi.ConfigSource;

/**
 * Config provider represents initialization context used to create new instance of Config again and again.
 */
class ProviderImpl implements Config.Context {

    private static final Logger LOGGER = Logger.getLogger(ConfigFactory.class.getName());

    private final List<Consumer<ConfigDiff>> listeners = new LinkedList<>();

    private final ConversionSupport conversionSupport;
    private final ConfigSourcesRuntime configSource;

    private final Executor changesExecutor;
    private final boolean keyResolving;

    private ConfigDiff lastConfigsDiff;
    private AbstractConfigImpl lastConfig;
    private boolean listening;

    ProviderImpl(ConversionSupport conversionSupport,
                 ConfigSourcesRuntime configSource,
                 Executor changesExecutor,
                 boolean keyResolving) {
        this.conversionSupport = conversionSupport;
        this.configSource = configSource;
        this.changesExecutor = changesExecutor;

        this.lastConfigsDiff = null;
        this.lastConfig = (AbstractConfigImpl) ConfigBuilderImpl.empty();

        this.keyResolving = keyResolving;
    }

    public synchronized AbstractConfigImpl newConfig() {
        lastConfig = build(configSource.load());

        if (!listening) {
            // only start listening for changes once the first config is built
            configSource.changeListener(objectNode -> rebuild(objectNode, false));
            configSource.startChanges();
            listening = true;
        }

        return lastConfig;
    }

    @Override
    public synchronized Config reload() {
        rebuild(configSource.latest(), true);
        return lastConfig;
    }

    @Override
    public synchronized Instant timestamp() {
        return lastConfig.context().timestamp();
    }

    @Override
    public synchronized Config last() {
        return lastConfig;
    }

    void onChange(Consumer<ConfigDiff> listener) {
        this.listeners.add(listener);
    }

    Iterable<ConfigSource> configSources() {
        return configSource.configSources();
    }

    private synchronized AbstractConfigImpl build(Optional<ObjectNode> rootNode) {

        // resolve tokens
        rootNode = rootNode.map(this::resolveKeys);

        // factory
        ConfigFactory factory = new ConfigFactory(conversionSupport,
                                                  rootNode.orElseGet(() -> ObjectNodeImpl.builder("").build()),
                                                  this);
        AbstractConfigImpl config = factory.config();

        return config;
    }

    private ObjectNode resolveKeys(ObjectNode rootNode) {
        Function<String, String> resolveTokenFunction = Function.identity();
        if (keyResolving) {
            Map<String, String> flattenValueNodes = ConfigUtils.flattenNodes(rootNode);

            if (flattenValueNodes.isEmpty()) {
                return rootNode;
            }

            Map<String, String> tokenValueMap = tokenToValueMap(flattenValueNodes);

            resolveTokenFunction = (token) -> {
                if (token.startsWith("$")) {
                    return tokenValueMap.get(parseTokenReference(token));
                }
                return token;
            };
        }
        return ObjectNodeImpl.builder("", resolveTokenFunction).members(rootNode).build();
    }

    private Map<String, String> tokenToValueMap(Map<String, String> flattenValueNodes) {
        return flattenValueNodes.keySet()
                .stream()
                .flatMap(this::tokensFromKey)
                .distinct()
                .collect(Collectors.toMap(Function.identity(), t ->
                        flattenValueNodes.compute(Key.unescapeName(t), (k, v) -> {
                            if (v == null) {
                                throw new IllegalStateException(String.format("Missing token '%s' to resolve.", t));
                            } else if (v.equals("")) {
                                throw new IllegalStateException(String.format("Missing value in token '%s' definition.", t));
                            } else if (v.startsWith("$")) {
                                throw new IllegalStateException(String.format(
                                        "Key token '%s' references to a reference in value. A recursive references is not "
                                                + "allowed.",
                                        t));
                            }
                            return v;
                        })));
    }

    private Stream<String> tokensFromKey(String s) {
        String[] tokens = s.split("\\.+(?![^(${)]*})");
        return Arrays.stream(tokens).filter(t -> t.startsWith("$")).map(ProviderImpl::parseTokenReference);
    }

    private static String parseTokenReference(String token) {
        if (token.startsWith("${") && token.endsWith("}")) {
            return token.substring(2, token.length() - 1);
        } else if (token.startsWith("$")) {
            return token.substring(1);
        }
        return token;
    }

    private synchronized void rebuild(Optional<ObjectNode> objectNode, boolean force) {
        // 1. build new Config
        AbstractConfigImpl newConfig = build(objectNode);
        // 2. for each subscriber fire event on specific node/key - see AbstractConfigImpl.FilteringConfigChangeEventSubscriber
        // 3. fire event
        ConfigDiff configsDiff = ConfigDiff.from(lastConfig, newConfig);
        if (!configsDiff.isEmpty()) {
            lastConfig = newConfig;
            lastConfigsDiff = configsDiff;

            fireLastChangeEvent();
        } else {
            if (force) {
                lastConfig = newConfig;
            }

            LOGGER.log(Level.FINER, "Change event is not fired, there is no change from the last load.");
        }
    }

    private void fireLastChangeEvent() {
        ConfigDiff configDiffs;

        synchronized (this) {
            configDiffs = this.lastConfigsDiff;
        }

        if (configDiffs != null) {
            LOGGER.log(Level.FINER, String.format("Firing event %s", configDiffs));

            changesExecutor.execute(() -> {
                for (Consumer<ConfigDiff> listener : listeners) {
                    listener.accept(configDiffs);
                }
            });
        }
    }
}

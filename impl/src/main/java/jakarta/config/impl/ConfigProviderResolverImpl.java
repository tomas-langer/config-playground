/*
 * Copyright (c) 2019, 2021 Oracle and/or its affiliates.
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
import java.util.concurrent.ConcurrentHashMap;

import jakarta.config.Config;
import jakarta.config.spi.ConfigBuilder;
import jakarta.config.spi.ConfigProviderResolver;

/**
 * Integration with Jakarta config.
 * This class is a {@link java.util.ServiceLoader} provider implementation.
 */
public class ConfigProviderResolverImpl extends ConfigProviderResolver {
    private static final Map<ClassLoader, Config> CONFIGS = new ConcurrentHashMap<>();

    @Override
    public Config getConfig() {
        return getConfig(Thread.currentThread().getContextClassLoader());
    }

    @Override
    public Config getConfig(ClassLoader classLoader) {
        ClassLoader loader;
        if (classLoader == null) {
            loader = Thread.currentThread().getContextClassLoader();
        } else {
            loader = classLoader;
        }
        return CONFIGS.computeIfAbsent(loader, it -> buildConfig(loader));
    }

    @Override
    public ConfigBuilder getBuilder() {
        return new ConfigBuilderImpl();
    }

    @Override
    public void registerConfig(Config config, ClassLoader classLoader) {
        CONFIGS.put(classLoader, config);
    }

    @Override
    public void releaseConfig(Config config) {
        ClassLoader loader = null;

        for (var entry : CONFIGS.entrySet()) {
            if (entry.getValue() == config) {
                loader = entry.getKey();
                break;
            }
        }

        if (loader == null) {
            // config is not registered
            return;
        }

        // only remove if we still have the same instance registered
        CONFIGS.remove(loader, config);
    }

    private Config buildConfig(ClassLoader loader) {
        return getBuilder()
            .forClassLoader(loader)
            .addDefaultSources()
            .addDiscoveredSources()
            .addDiscoveredConverters()
            .build();
    }
}

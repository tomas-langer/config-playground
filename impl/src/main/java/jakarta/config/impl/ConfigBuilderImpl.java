/*
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.
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

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

import jakarta.config.Config;
import jakarta.config.spi.ConfigBuilder;
import jakarta.config.spi.ConfigParser;
import jakarta.config.spi.ConfigSource;
import jakarta.config.spi.ConfigSourceProvider;
import jakarta.config.spi.Converter;

/**
 * Configuration builder.
 */
class ConfigBuilderImpl implements ConfigBuilder {
    private static final String DEFAULT_CONFIG_SOURCE_PREFIX = "META-INF/jakarta-config.";

    private static final List<ConfigParser> PARSERS;
    private static final Set<String> SUPPORTED_SUFFIXES;

    static {
        ServiceLoader<ConfigParser> services = ServiceLoader.load(ConfigParser.class);

        PARSERS = services.stream()
            .map(ServiceLoader.Provider::get)
            .sorted((o1, o2) -> {
                int p1 = Priorities.find(o1, ConfigParser.PRIORITY);
                int p2 = Priorities.find(o2, ConfigParser.PRIORITY);
                return Integer.compare(p1, p2);
            }).collect(Collectors.toCollection(LinkedList::new));

        // can be a constant
        Set<String> supportedSuffixes = new LinkedHashSet<>();
        PARSERS.forEach(it -> supportedSuffixes.addAll(it.supportedSuffixes()));
        SUPPORTED_SUFFIXES = supportedSuffixes;
    }

    private final List<OrdinalConfigSource> sources = new LinkedList<>();
    private final List<OrdinalConverter> converters = new LinkedList<>();

    private ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

    private boolean useDefaultSources = false;
    private boolean useDiscoveredSources = false;
    private boolean useDiscoveredConverters = false;
    private String profile;
    private ScheduledExecutorService changesExecutor;
    private MergingStrategy mergingStrategy = new FallbackMergingStrategy();

    ConfigBuilderImpl() {
    }

    @Override
    public Config build() {
        ScheduledExecutorService executor = changesExecutor;
        if (executor == null) {
            executor = Executors.newSingleThreadScheduledExecutor(new ConfigThreadFactory("config-changes"));
        }
        // the build method MUST NOT modify builder state, as it may be called more than once
        // there are three lists used by the configuration:
        //  sources
        //  converters
        //  filters
        List<OrdinalConfigSource> ordinalSources = new LinkedList<>(sources);
        List<OrdinalConverter> ordinalConverters = new LinkedList<>(converters);

        /*
         Converters
         */
        ConfigConverters.addBuiltInConverters(ordinalConverters);
        if (useDiscoveredConverters) {
            addDiscoveredConverters(ordinalConverters);
        }

        /*
         Config sources
         */
        if (useDefaultSources) {
            addDefaultSources(ordinalSources);
        }
        if (useDiscoveredSources) {
            addDiscoveredSources(ordinalSources);
        }

        ConfigSourceContextImpl context = new ConfigSourceContextImpl(executor, PARSERS);
        for (OrdinalConfigSource ordinalSource : ordinalSources) {
            ordinalSource.configSource().init(context);
        }

        List<ConfigSourceRuntimeImpl> sourceRuntimes = new LinkedList<>();

        for (OrdinalConfigSource ordinalSource : ordinalSources) {
            sourceRuntimes.add(context.sourceRuntime(ordinalSource.configSource(), ordinalSource.priority()));
        }

        for (ConfigSourceRuntimeImpl runtime : sourceRuntimes) {
            runtime.load();
        }

        // now it is from lowest to highest
        sourceRuntimes.sort(Comparator.comparingInt(ConfigSourceRuntimeImpl::priority));
        ordinalConverters.sort(Comparator.comparingInt(OrdinalConverter::getPriority));

        HashMap<Class<?>, Converter<?>> targetConverters = new HashMap<>();
        ordinalConverters.forEach(ordinal -> targetConverters.putIfAbsent(ordinal.getType(), ordinal.getConverter()));

        ConfigSourcesRuntime configSources = new ConfigSourcesRuntime(sourceRuntimes, mergingStrategy);

        AbstractConfigImpl result = new ProviderImpl(new ConversionSupport(targetConverters),
                         configSources,
                         executor,
                         true)
            .newConfig();

        // if we already have a profile configured, we have loaded it and can safely return
        if (profile != null) {
            return result;
        }

        // let's see if there is a profile configured
        String configuredProfile = result.getOptionalValue("mp.config.profile", String.class).orElse(null);

        // nope, return the result
        if (configuredProfile == null) {
            return result;
        }

        // yes, update it and re-build with profile information
        profile(configuredProfile);

        return build();
    }

    @Override
    public ConfigBuilder addDefaultSources() {
        useDefaultSources = true;
        return this;
    }

    @Override
    public ConfigBuilder addDiscoveredSources() {
        useDiscoveredSources = true;
        return this;
    }

    @Override
    public ConfigBuilder addDiscoveredConverters() {
        useDiscoveredConverters = true;
        return this;
    }

    @Override
    public ConfigBuilder forClassLoader(ClassLoader loader) {
        this.classLoader = loader;
        return this;
    }

    @Override
    public ConfigBuilder withSources(ConfigSource... sources) {
        for (ConfigSource source : sources) {
            this.sources.add(new OrdinalConfigSource(source));
        }
        return this;
    }

    @Override
    public <T> ConfigBuilder withConverter(Class<T> aClass, int ordinal, Converter<T> converter) {
        this.converters.add(new OrdinalConverter(converter, aClass, ordinal));
        return this;
    }

    @Override
    public ConfigBuilder withConverters(Converter<?>... converters) {
        for (Converter<?> converter : converters) {
            this.converters.add(new OrdinalConverter(converter));
        }
        return this;
    }

    @Override
    public ConfigBuilder profile(String profile) {
        this.profile = profile;
        return this;
    }

    private void addDiscoveredSources(List<OrdinalConfigSource> targetConfigSources) {
        ServiceLoader.load(ConfigSource.class)
            .forEach(it -> targetConfigSources.add(new OrdinalConfigSource(it)));

        ServiceLoader.load(ConfigSourceProvider.class)
            .forEach(it -> it.getConfigSources(classLoader)
                .forEach(source -> targetConfigSources.add(new OrdinalConfigSource(source))));
    }

    private void addDiscoveredConverters(List<OrdinalConverter> targetConverters) {
        ServiceLoader.load(Converter.class)
            .forEach(it -> targetConverters.add(new OrdinalConverter(it)));
    }

    private void addDefaultSources(List<OrdinalConfigSource> targetConfigSources) {
        if (useDefaultSources) {
            // add default sources - system properties, environment variables and microprofile-config.properties
            targetConfigSources.add(new OrdinalConfigSource(ConfigSources.systemProperties(), 50));
            targetConfigSources.add(new OrdinalConfigSource(ConfigSources.environmentVariables(), 70));
            // jakarta-config.properties

            if (profile == null) {
                SUPPORTED_SUFFIXES.forEach(suffix -> ConfigSources.classPath(classLoader, DEFAULT_CONFIG_SOURCE_PREFIX + suffix)
                    .stream()
                    .map(OrdinalConfigSource::new)
                    .forEach(targetConfigSources::add));

            } else {
                SUPPORTED_SUFFIXES.forEach(suffix -> ConfigSources.classPath(classLoader,
                                                                             DEFAULT_CONFIG_SOURCE_PREFIX + suffix,
                                                                             profile)
                    .stream()
                    .map(OrdinalConfigSource::new)
                    .forEach(targetConfigSources::add));
            }
        }
    }

    static Config empty() {
        return EmptyConfigHolder.EMPTY;
    }

    /**
     * Holds single instance of empty Config.
     */
    static final class EmptyConfigHolder {
        private EmptyConfigHolder() {
            throw new AssertionError("Instantiation not allowed.");
        }

        static final Config EMPTY = new ConfigBuilderImpl()
            // the empty config configSource is needed, so we do not look for meta config or default
            // config sources
            .build();

    }
}

/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
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

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Logger;

import jakarta.config.ReservedKeys;
import jakarta.config.spi.ChangeEventType;
import jakarta.config.spi.ConfigNode;
import jakarta.config.spi.ConfigNode.ObjectNode;
import jakarta.config.spi.ConfigSource;
import jakarta.config.spi.ConfigSourceRuntime;
import jakarta.config.spi.EventConfigSource;
import jakarta.config.spi.LazyConfigSource;
import jakarta.config.spi.NodeConfigSource;
import jakarta.config.spi.ParsableConfigSource;
import jakarta.config.spi.PollableConfigSource;
import jakarta.config.spi.PollingStrategy;

class ConfigSourceRuntimeImpl implements ConfigSourceRuntime {
    private static final Logger LOGGER = Logger.getLogger(ConfigSourceRuntimeImpl.class.getName());

    private final List<BiConsumer<String, ConfigNode>> listeners = new LinkedList<>();
    private final ConfigSourceContextImpl context;
    private final Function<KeyImpl, Optional<ConfigNode>> singleNodeFunction;
    private final Supplier<Optional<ObjectNode>> reloader;
    private final ConfigSource configSource;
    private final int priority;
    private final boolean isLazy;
    private final boolean changesSupported;
    private final Runnable changesRunnable;

    // for eager sources, this is the data we get initially, everything else is handled through change listeners
    private Optional<ObjectNode> initialData;
    private Map<KeyImpl, ConfigNode> loadedData;
    private boolean dataLoaded = false;
    private Integer loadedPriority;
    // we only want to start change support if somebody listens for changes
    private volatile boolean changesWanted = false;
    // set to true if changes started
    private volatile boolean changesStarted = false;
    private Boolean pollingEnabled;
    private Duration pollingDuration;

    @SuppressWarnings("unchecked")
    ConfigSourceRuntimeImpl(ConfigSourceContextImpl context, ConfigSource configSource, int priority) {
        this.context = context;
        this.configSource = configSource;
        this.priority = priority;

        Supplier<Optional<ObjectNode>> reloader;
        Function<KeyImpl, Optional<ConfigNode>> singleNodeFunction;
        boolean lazy = false;
        // change support
        AtomicReference<Object> lastStamp = new AtomicReference<>();

        if (configSource instanceof ParsableConfigSource) {
            // eager parsable config configSource
            reloader = new ParsableConfigSourceReloader(context,
                                                        (ParsableConfigSource) configSource,
                                                        lastStamp,
                                                        this::priority);
            singleNodeFunction = objectNodeToSingleNode();
        } else if (configSource instanceof NodeConfigSource) {
            // eager node config configSource
            reloader = new NodeConfigSourceReloader((NodeConfigSource) configSource, lastStamp, this::priority);
            singleNodeFunction = objectNodeToSingleNode();
        } else if (configSource instanceof LazyConfigSource) {
            LazyConfigSource lazySource = (LazyConfigSource) configSource;
            // lazy config configSource
            reloader = Optional::empty;
            singleNodeFunction = key -> {
                Optional<ConfigNode> node = lazySource.node(key.toString());
                node.ifPresent(it -> {
                    it.configSource(lazySource);
                    it.sourcePriority(this.priority());
                });
                return node;
            };
            lazy = true;
        } else {
            throw new IllegalStateException("Config configSource " + configSource
                                                + ", class: " + configSource.getClass().getName()
                                                + ", name: " + configSource.getName()
                                                + " does not "
                                                + "implement any of required interfaces. A config configSource must at least "
                                                + "implement one of the following: ParsableSource, or NodeConfigSource, or "
                                                + "LazyConfigSource");
        }

        this.isLazy = lazy;
        this.reloader = reloader;
        this.singleNodeFunction = singleNodeFunction;

        boolean changesSupported = false;
        Runnable changesRunnable = null;

        if (configSource instanceof PollableConfigSource) {
            PollableConfigSource<Object> pollable = (PollableConfigSource<Object>) configSource;

            changesSupported = true;
            changesRunnable = new PollingStrategyStarter(context,
                                                         listeners,
                                                         reloader,
                                                         configSource,
                                                         pollable,
                                                         this::pollingEnabled,
                                                         this::pollingDuration,
                                                         context.changesExecutor(),
                                                         lastStamp);
        }

        if (!changesSupported && (configSource instanceof EventConfigSource)) {
            EventConfigSource event = (EventConfigSource) configSource;
            changesSupported = true;
            changesRunnable = () -> event.onChange((key, config) -> listeners.forEach(it -> it.accept(key, config)));
        }

        this.changesRunnable = changesRunnable;
        this.changesSupported = changesSupported;
    }

    @Override
    public synchronized void onChange(BiConsumer<String, ConfigNode> change) {
        if (!dataLoaded) {
            throw new IllegalStateException("Cannot start changes before initial data load");
        }
        if (!changesSupported) {
            return;
        }

        this.listeners.add(change);
        this.changesWanted = true;
        startChanges();
    }

    @Override
    public ConfigSource configSource() {
        return configSource;
    }

    @Override
    public boolean isLazy() {
        return isLazy;
    }

    @Override
    public String toString() {
        return priority() + ": " + configSource.getName() + " runtime";
    }

    @Override
    public int hashCode() {
        return Objects.hash(configSource);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }
        ConfigSourceRuntimeImpl that = (ConfigSourceRuntimeImpl) o;
        return configSource.equals(that.configSource);
    }

    @Override
    public Optional<ConfigNode> node(String key) {
        return singleNodeFunction.apply(KeyImpl.of(key));
    }

    @Override
    public int priority() {
        if (loadedPriority == null) {
            return priority;
        }
        return loadedPriority;
    }

    @Override
    public synchronized Optional<ObjectNode> load() {
        if (dataLoaded) {
            throw new RuntimeException("Attempting to load a single config configSource multiple times. This is a bug.");
        }

        initialLoad();

        this.loadedPriority = this.node(ReservedKeys.CONFIG_PRIORITY)
            .flatMap(it -> it.value().map(Integer::parseInt))
            .orElse(null);

        // now we have loaded priority, we need to update the tree with the current priority
        if (loadedPriority != null) {
            this.initialData.ifPresent(it -> it.sourcePriority(loadedPriority));
        }

        this.pollingEnabled = this.node(ReservedKeys.CONFIG_POLLING_ENABLED)
            .flatMap(it -> it.value().map(Boolean::parseBoolean))
            .orElse(true);

        this.pollingDuration = this.node(ReservedKeys.CONFIG_POLLING_DURATION)
            .flatMap(it -> it.value().map(Duration::parse))
            .orElseGet(() -> Duration.ofSeconds(10));

        return this.initialData;
    }

    Optional<ObjectNode> initialData() {
        if (!dataLoaded) {
            throw new RuntimeException("Config configSource should have been loaded. This is a bug.");
        }
        return this.initialData;
    }

    boolean changesSupported() {
        return changesSupported;
    }

    private void startChanges() {
        if (!changesStarted && dataLoaded && changesWanted) {
            changesStarted = true;
            changesRunnable.run();
        }
    }

    private void initialLoad() {
        if (dataLoaded) {
            return;
        }

        Optional<ObjectNode> loadedData = reloader.get();

        if (loadedData.isEmpty() && !configSource.optional() && !isLazy()) {
            throw new IllegalStateException("Cannot load data from mandatory configSource: " + configSource);
        }

        this.initialData = loadedData;

        this.loadedData = new HashMap<>();

        initialData.ifPresent(data -> {
            Map<KeyImpl, ConfigNode> keyToNodeMap = ConfigUtils.createFullKeyToNodeMap(data);

            this.loadedData.putAll(keyToNodeMap);
        });

        dataLoaded = true;
    }

    private Boolean pollingEnabled() {
        return pollingEnabled;
    }

    private Duration pollingDuration() {
        return pollingDuration;
    }

    private Function<KeyImpl, Optional<ConfigNode>> objectNodeToSingleNode() {
        return key -> {
            if (loadedData == null) {
                throw new IllegalStateException("Single node of an eager configSource requested before load method was called."
                                                    + " This is a bug.");
            }

            return Optional.ofNullable(loadedData.get(key));
        };
    }

    private static final class PollingStrategyStarter implements Runnable {
        private static final Logger LOGGER = Logger.getLogger(PollingStrategyStarter.class.getName());

        private final PollingStrategyListener listener;
        private final ConfigSource source;
        private final Supplier<Boolean> pollingEnabledSupplier;
        private final Supplier<Duration> pollingDurationSupplier;
        private final ScheduledExecutorService executor;

        private PollingStrategyStarter(ConfigSourceContextImpl configContext,
                                       List<BiConsumer<String, ConfigNode>> listeners,
                                       Supplier<Optional<ObjectNode>> reloader,
                                       ConfigSource source,
                                       PollableConfigSource<Object> pollable,
                                       Supplier<Boolean> pollingEnabledSupplier,
                                       Supplier<Duration> pollingDurationSupplier,
                                       ScheduledExecutorService executor,
                                       AtomicReference<Object> lastStamp) {
            this.source = source;
            this.pollingEnabledSupplier = pollingEnabledSupplier;
            this.pollingDurationSupplier = pollingDurationSupplier;
            this.executor = executor;
            this.listener = new PollingStrategyListener(configContext, listeners, reloader, source, pollable, lastStamp);
        }

        @Override
        public void run() {
            boolean enabled = pollingEnabledSupplier.get();

            if (enabled) {
                Duration duration = pollingDurationSupplier.get();
                LOGGER.finest(() -> "Polling strategy is enabled with polling duration of: " + duration
                    + " for source " + source.getName());
                PollingStrategy strategy = new RegularPollingStrategy(executor, duration);
                strategy.start(listener);
            } else {
                LOGGER.finest(() -> "Polling strategy is disabled for source " + source.getName());
            }
        }
    }

    private static final class PollingStrategyListener implements PollingStrategy.Polled {

        private final ConfigSourceContextImpl configContext;
        private final List<BiConsumer<String, ConfigNode>> listeners;
        private final Supplier<Optional<ObjectNode>> reloader;
        private final ConfigSource source;
        private final PollableConfigSource<Object> pollable;
        private final AtomicReference<Object> lastStamp;

        private PollingStrategyListener(ConfigSourceContextImpl configContext,
                                        List<BiConsumer<String, ConfigNode>> listeners,
                                        Supplier<Optional<ObjectNode>> reloader,
                                        ConfigSource source,
                                        PollableConfigSource<Object> pollable,
                                        AtomicReference<Object> lastStamp) {

            this.configContext = configContext;
            this.listeners = listeners;
            this.reloader = reloader;
            this.source = source;
            this.pollable = pollable;
            this.lastStamp = lastStamp;
        }

        private static void triggerChanges(ConfigSourceContextImpl configContext,
                                           List<BiConsumer<String, ConfigNode>> listeners,
                                           Optional<ObjectNode> objectNode) {

            configContext.changesExecutor()
                .execute(() -> {
                    for (BiConsumer<String, ConfigNode> listener : listeners) {
                        listener.accept("", objectNode.orElse(ObjectNodeImpl.builder("").build()));
                    }
                });

        }

        @Override
        public String toString() {
            return "config source: " + source.getName();
        }

        @Override
        public ChangeEventType poll(Instant when) {
            Object lastStampValue = lastStamp.get();
            if ((null == lastStampValue) || pollable.isModified(lastStampValue)) {
                Optional<ObjectNode> objectNode = reloader.get();
                if (objectNode.isEmpty()) {
                    if (source.optional()) {
                        // this is a valid change
                        triggerChanges(configContext, listeners, objectNode);
                    } else {
                        LOGGER.info("Mandatory config configSource is not available, ignoring change.");
                    }
                    return ChangeEventType.DELETED;
                } else {
                    triggerChanges(configContext, listeners, objectNode);
                    return ChangeEventType.CHANGED;
                }
            }
            return ChangeEventType.UNCHANGED;
        }
    }
}

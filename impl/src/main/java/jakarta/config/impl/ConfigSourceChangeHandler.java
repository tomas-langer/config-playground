package jakarta.config.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.config.ReservedKeys;
import jakarta.config.spi.ChangeEventType;
import jakarta.config.spi.ChangeWatcher;
import jakarta.config.spi.ChangeWatcherProvider;
import jakarta.config.spi.ConfigNode;
import jakarta.config.spi.ConfigNode.ObjectNode;
import jakarta.config.spi.ConfigSource;
import jakarta.config.spi.EventConfigSource;
import jakarta.config.spi.PollableConfigSource;
import jakarta.config.spi.PollingStrategy;
import jakarta.config.spi.WatchableConfigSource;

interface ConfigSourceChangeHandler {

    static ConfigSourceChangeHandler create(ConfigSourceContextImpl context,
                                            ConfigSource configSource,
                                            ConfigSourceDataLoader sourceDataLoader,
                                            List<BiConsumer<String, ConfigNode>> listeners) {
        if (configSource instanceof PollableConfigSource || configSource instanceof WatchableConfigSource) {
            return new ChangeHandler(context, configSource, sourceDataLoader, listeners);
        }

        return new ConfigSourceChangeHandler() { };
    }

    // called exactly once when data is loaded and we can safely process change setup
    default void init() {
    }

    default boolean changesSupported() {
        return false;
    }

    default void startChanges() {
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

    final class PollingStrategyStarter implements Runnable {
        private static final Logger LOGGER = Logger.getLogger(PollingStrategyStarter.class.getName());

        private final PollingStrategyListener listener;
        private final ConfigSource source;
        private final Duration pollingDuration;
        private final ScheduledExecutorService executor;

        private PollingStrategyStarter(ConfigSourceContextImpl configContext,
                                       List<BiConsumer<String, ConfigNode>> listeners,
                                       ConfigSourceDataLoader dataLoader,
                                       ConfigSource source,
                                       PollableConfigSource<Object> pollable,
                                       Duration pollingDuration,
                                       ScheduledExecutorService executor) {
            this.source = source;
            this.pollingDuration = pollingDuration;
            this.executor = executor;
            this.listener = new PollingStrategyListener(configContext, listeners, dataLoader, source, pollable);
        }

        @Override
        public void run() {
            LOGGER.finest(() -> "Polling strategy is enabled with polling duration of: " + pollingDuration
                + " for source " + source.getName());

            PollingStrategy strategy = new RegularPollingStrategy(executor, pollingDuration);
            strategy.start(listener);
        }
    }

    final class PollingStrategyListener implements PollingStrategy.Polled {
        private static final Logger LOGGER = Logger.getLogger(PollingStrategyListener.class.getName());

        private final ConfigSourceContextImpl configContext;
        private final List<BiConsumer<String, ConfigNode>> listeners;
        private final ConfigSourceDataLoader dataLoader;
        private final ConfigSource source;
        private final PollableConfigSource<Object> pollable;
        private final AtomicReference<Object> lastStamp = new AtomicReference<>();

        public PollingStrategyListener(ConfigSourceContextImpl configContext,
                                       List<BiConsumer<String, ConfigNode>> listeners,
                                       ConfigSourceDataLoader dataLoader,
                                       ConfigSource source,
                                       PollableConfigSource<Object> pollable) {

            this.configContext = configContext;
            this.listeners = listeners;
            this.dataLoader = dataLoader;
            this.source = source;
            this.pollable = pollable;

            this.lastStamp.set(dataLoader.lastStamp());
        }

        @Override
        public String toString() {
            return "config source: " + source.getName();
        }

        @Override
        public ChangeEventType poll(Instant when) {
            Object lastStampValue = lastStamp.get();
            if ((null == lastStampValue) || pollable.isModified(lastStampValue)) {
                Optional<ObjectNode> objectNode = dataLoader.reload();
                if (objectNode.isEmpty()) {
                    if (source.optional()) {
                        lastStamp.set(dataLoader.lastStamp());
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

    class ChangeHandler implements ConfigSourceChangeHandler {
        private static final Logger LOGGER = Logger.getLogger(ChangeHandler.class.getName());
        private static final List<ChangeWatcherProvider> CHANGE_WATCHER_PROVIDERS = PrioritizedServiceLoader.create(
                ChangeWatcherProvider.class).asList();
        private static final Map<Class<Object>, ChangeWatcher<Object>> CHANGE_WATCHER_CACHE = new IdentityHashMap<>();

        private final ConfigSourceContextImpl context;
        private final ConfigSource configSource;
        private final ConfigSourceDataLoader sourceDataLoader;
        private final List<BiConsumer<String, ConfigNode>> listeners;

        private Runnable changesRunnable;
        private boolean changesSupported;

        private ChangeHandler(ConfigSourceContextImpl context,
                              ConfigSource configSource,
                              ConfigSourceDataLoader sourceDataLoader,
                              List<BiConsumer<String, ConfigNode>> listeners) {
            this.context = context;
            this.configSource = configSource;
            this.sourceDataLoader = sourceDataLoader;
            this.listeners = listeners;
        }

        @Override
        public void init() {
            if (!asBoolean(sourceDataLoader.get(Key.create(ReservedKeys.CHANGE_SUPPORT_ENABLED)), true)) {
                // change support disabled
                LOGGER.finest("Config source " + configSource.getName() + " has change support explicitly disabled");
                return;
            }
            if (configSource instanceof WatchableConfigSource
                && asBoolean(sourceDataLoader.get(Key.create(ReservedKeys.CHANGE_WATCHER_ENABLED)), true)) {
                WatchableConfigSource<Object> watchable = (WatchableConfigSource<Object>) configSource;
                changesSupported = true;
                findChangeWatcher(watchable.changeWatcherType(), context.changesExecutor())
                    .ifPresent(it -> changesRunnable = new WatcherStarter(context,
                                                                      listeners,
                                                                      sourceDataLoader,
                                                                      watchable,
                                                                      it));

            }
            if (!changesSupported && configSource instanceof PollableConfigSource
                && asBoolean(sourceDataLoader.get(Key.create(ReservedKeys.CONFIG_POLLING_ENABLED)), true)) {

                Duration pollingDuration = asDuration(sourceDataLoader.get(Key.create(ReservedKeys.CONFIG_POLLING_DURATION)),
                                                      Duration.ofMinutes(1));
                PollableConfigSource<Object> pollable = (PollableConfigSource<Object>) configSource;

                changesSupported = true;
                changesRunnable = new PollingStrategyStarter(context,
                                                             listeners,
                                                             sourceDataLoader,
                                                             configSource,
                                                             pollable,
                                                             pollingDuration,
                                                             context.changesExecutor());
            }
            if (!changesSupported && configSource instanceof EventConfigSource) {
                EventConfigSource event = (EventConfigSource) configSource;
                changesSupported = true;
                changesRunnable = () -> event.onChange((key, config) -> listeners.forEach(it -> it.accept(key, config)));
            }

            LOGGER.finest("Config source " + configSource.getName() + " has change support "
                              + (changesSupported ? "enabled" : "disabled"));
        }

        @Override
        public boolean changesSupported() {
            return changesSupported;
        }

        @Override
        public void startChanges() {
            if (changesRunnable != null) {
                changesRunnable.run();
            }
        }

        private static Optional<ChangeWatcher<Object>> findChangeWatcher(Class<Object> type,
                                                                         ScheduledExecutorService executor) {
            ChangeWatcher<Object> watcher = CHANGE_WATCHER_CACHE.get(type);
            if (watcher != null) {
                return Optional.of(watcher);
            }

            return CHANGE_WATCHER_PROVIDERS.stream()
                .filter(it -> it.support(type))
                .findFirst()
                .map(it -> {
                    ChangeWatcher<Object> newWatcher = it.create(type, executor);
                    if (it.reusable()) {
                        CHANGE_WATCHER_CACHE.put(type, newWatcher);
                    }
                    return newWatcher;
                });
        }

        private Duration asDuration(Optional<ConfigNode> configNode, Duration defaultValue) {
            return configNode
                .flatMap(ConfigNode::value)
                .map(it -> {
                    try {
                        return Duration.parse(it);
                    } catch (Exception e) {
                        throw new IllegalArgumentException("Property "
                                                               + ReservedKeys.CONFIG_POLLING_DURATION
                                                               + " must represent a duration (see java.time.Duration), but is "
                                                               + it
                                                               + " in source " + configSource.getName());
                    }
                })
                .orElse(defaultValue);
        }

        private boolean asBoolean(Optional<ConfigNode> configNode, boolean defaultValue) {
            return configNode
                .flatMap(ConfigNode::value)
                .map(Boolean::parseBoolean)
                .orElse(defaultValue);
        }

        private static class WatcherStarter implements Runnable {
            private final WatchableListener listener;
            private final WatchableConfigSource<Object> watchable;
            private final ChangeWatcher<Object> changeWatcher;

            private WatcherStarter(ConfigSourceContextImpl context,
                                   List<BiConsumer<String, ConfigNode>> listeners,
                                   ConfigSourceDataLoader sourceDataLoader,
                                   WatchableConfigSource<Object> watchable,
                                   ChangeWatcher<Object> changeWatcher) {
                this.listener = new WatchableListener(context, listeners, sourceDataLoader, watchable);
                this.watchable = watchable;
                this.changeWatcher = changeWatcher;
            }

            @Override
            public void run() {
                LOGGER.finest(() -> "Change watcher is enabled for source " + watchable.getName()
                    + " with type: " + watchable.changeWatcherType().getName()
                    + " and target: " + watchable.changeWatcherTarget());

                Object target = watchable.changeWatcherTarget();
                changeWatcher.start(target, listener);
            }
        }
    }

    final class WatchableListener implements Consumer<ChangeWatcher.ChangeEvent<Object>> {
        private static final Logger LOGGER = Logger.getLogger(WatchableListener.class.getName());
        private final ConfigSourceContextImpl context;
        private final List<BiConsumer<String, ConfigNode>> listeners;
        private final ConfigSourceDataLoader sourceDataLoader;
        private final ConfigSource configSource;


        private WatchableListener(ConfigSourceContextImpl context,
                                 List<BiConsumer<String, ConfigNode>> listeners,
                                 ConfigSourceDataLoader sourceDataLoader,
                                 ConfigSource configSource) {

            this.context = context;
            this.listeners = listeners;
            this.sourceDataLoader = sourceDataLoader;
            this.configSource = configSource;
        }

        @Override
        public void accept(ChangeWatcher.ChangeEvent<Object> change) {
            try {
                Optional<ObjectNode> objectNode = sourceDataLoader.reload();
                if (objectNode.isEmpty()) {
                    if (configSource.optional()) {
                        // this is a valid change
                        triggerChanges(context, listeners, objectNode);
                    } else {
                        LOGGER.info("Mandatory config source " + configSource.getName()
                                        + " is not available, ignoring change.");
                    }
                } else {
                    triggerChanges(context, listeners, objectNode);
                }
            } catch (Exception e) {
                LOGGER.info("Failed to reload config source "
                                + configSource.getName()
                                + ", exception available in finest log level. "
                                + "Change that triggered this event: "
                                + change);
                LOGGER.log(Level.FINEST, "Failed to reload config source", e);
            }
        }
    }
}

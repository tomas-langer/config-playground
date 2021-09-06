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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.logging.Logger;

import jakarta.config.spi.ConfigNode;
import jakarta.config.spi.ConfigNode.ObjectNode;
import jakarta.config.spi.ConfigSource;
import jakarta.config.spi.ConfigSourceRuntime;

class ConfigSourceRuntimeImpl implements ConfigSourceRuntime {
    private static final Logger LOGGER = Logger.getLogger(ConfigSourceRuntimeImpl.class.getName());

    private final List<BiConsumer<String, ConfigNode>> listeners = new LinkedList<>();
    private final ConfigSourceContextImpl context;
    private final ConfigSource configSource;
    // set to true if changes started
    private final AtomicBoolean changesStarted = new AtomicBoolean();
    private final AtomicBoolean dataLoaded = new AtomicBoolean();
    private final ConfigSourceDataLoader sourceDataLoader;
    private final ConfigSourceChangeHandler changeHandler;

    // for eager sources, this is the data we get initially, everything else is handled through change listeners
    private Optional<ObjectNode> initialData;
    private Map<KeyImpl, ConfigNode> loadedData;

    // we only want to start change support if somebody listens for changes
    private volatile boolean changesWanted = false;

    ConfigSourceRuntimeImpl(ConfigSourceContextImpl context, ConfigSource configSource, int priority) {
        this.context = context;
        this.configSource = configSource;
        this.sourceDataLoader = ConfigSourceDataLoader.create(context, configSource, priority);
        this.changeHandler = ConfigSourceChangeHandler.create(context, configSource, sourceDataLoader, listeners);
    }

    @Override
    public synchronized void onChange(BiConsumer<String, ConfigNode> change) {
        if (!dataLoaded.get()) {
            throw new IllegalStateException("Cannot start changes before initial data load");
        }

        if (!changeHandler.changesSupported()) {
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
        return sourceDataLoader.isLazy();
    }

    @Override
    public String toString() {
        return sourceDataLoader.priority() + ": " + configSource.getName() + " runtime";
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
        return sourceDataLoader.get(KeyImpl.of(key));
    }

    @Override
    public int priority() {
        return sourceDataLoader.priority();
    }

    @Override
    public synchronized Optional<ObjectNode> load() {
        if (!dataLoaded.compareAndSet(false, true)) {
            throw new RuntimeException("Attempting to load a single config configSource multiple times ("
                                           + configSource.getName() + "). This is a bug.");
        }

        initialLoad();

        changeHandler.init();

        return this.initialData;
    }

    Optional<ObjectNode> initialData() {
        if (!dataLoaded.get()) {
            throw new RuntimeException("Config configSource should have been loaded. This is a bug.");
        }
        return this.initialData;
    }

    /**
     * This method is only useful after {@link #initialLoad()}.
     *
     * @return
     */
    boolean changesSupported() {
        if (changeHandler == null) {
            throw new IllegalStateException("Cannot determine whether changes are supported before initial data load.");
        }
        return changeHandler.changesSupported();
    }

    private void startChanges() {
        if (dataLoaded.get()
            && changesWanted
            && changesStarted.compareAndSet(false, true)) {
            changeHandler.startChanges();
        }
    }

    private void initialLoad() {
        Optional<ObjectNode> loadedData = sourceDataLoader.load();

        if (loadedData.isEmpty() && !configSource.optional() && !isLazy()) {
            throw new IllegalStateException("Cannot load data from mandatory configSource: " + configSource);
        }

        this.initialData = loadedData;
        this.loadedData = new HashMap<>();

        initialData.ifPresent(data -> {
            Map<KeyImpl, ConfigNode> keyToNodeMap = ConfigUtils.createFullKeyToNodeMap(data);

            this.loadedData.putAll(keyToNodeMap);
        });
    }
}

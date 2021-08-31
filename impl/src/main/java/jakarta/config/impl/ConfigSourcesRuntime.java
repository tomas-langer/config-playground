package jakarta.config.impl;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.config.spi.ConfigNode;
import jakarta.config.spi.ConfigNode.ObjectNode;
import jakarta.config.spi.ConfigSource;

final class ConfigSourcesRuntime {

    private final List<RuntimeWithData> loadedData = new LinkedList<>();

    private final List<ConfigSourceRuntimeImpl> allSources;
    private final List<ConfigSource> configSources;
    private final MergingStrategy mergingStrategy;

    private volatile Consumer<Optional<ObjectNode>> changeListener;

    ConfigSourcesRuntime(List<ConfigSourceRuntimeImpl> allSources,
                         MergingStrategy mergingStrategy) {
        this.allSources = allSources;
        this.mergingStrategy = mergingStrategy;
        this.configSources = allSources.stream()
            .map(ConfigSourceRuntimeImpl::source)
            .collect(Collectors.toList());
    }

    void changeListener(Consumer<Optional<ObjectNode>> changeListener) {
        this.changeListener = changeListener;
    }

    void startChanges() {
        loadedData.stream()
            .filter(loaded -> loaded.runtime().changesSupported())
            .forEach(loaded -> loaded.runtime()
                .onChange((key, configNode) -> {
                    loaded.data(processChange(loaded.data, key, configNode));
                    changeListener.accept(latest());
                }));
    }

    private Optional<ObjectNode> processChange(Optional<ObjectNode> oldData, String changedKey, ConfigNode changeNode) {
        ObjectNode changeObjectNode = toObjectNode(changeNode);

        if (changedKey.isEmpty()) {
            // we have a root, no merging with original data, just return it
            return Optional.of(changeObjectNode);
        }

        ObjectNode newRootNode = ObjectNodeImpl.builder("")
            .addObject(changedKey, changeObjectNode)
            .build();

        // old data was empty, this is the only data we have
        if (oldData.isEmpty()) {
            return Optional.of(newRootNode);
        }

        // we had data, now we have new data (not on root), let's merge
        return Optional.of(mergingStrategy.merge(List.of(newRootNode, oldData.get())));
    }

    private ObjectNode toObjectNode(ConfigNode changeNode) {
        switch (changeNode.nodeType()) {
        case OBJECT:
            return (ObjectNode) changeNode;
        case LIST:
            return ObjectNodeImpl.builder(changeNode.key())
                .addList("", (ConfigNode.ListNode) changeNode).build();
        case VALUE:
            return ObjectNodeImpl.builder(changeNode.key())
                .value(((ConfigNode.ValueNode) changeNode).get()).build();
        default:
            throw new IllegalArgumentException("Unsupported node type: " + changeNode.nodeType());
        }
    }

    synchronized Optional<ObjectNode> latest() {
        List<ObjectNode> objectNodes = loadedData.stream()
            .map(RuntimeWithData::data)
            .flatMap(Optional::stream)
            .collect(Collectors.toList());

        return Optional.of(mergingStrategy.merge(objectNodes));
    }

    synchronized Optional<ObjectNode> load() {

        for (ConfigSourceRuntimeImpl source : allSources) {
            if (source.isLazy()) {
                loadedData.add(new RuntimeWithData(source, Optional.empty()));
            } else {
                loadedData.add(new RuntimeWithData(source, source.initialData()
                    .map(ObjectNodeImpl::wrap)));
            }
        }

        Set<KeyImpl> allKeys = loadedData.stream()
            .map(RuntimeWithData::data)
            .flatMap(Optional::stream)
            .flatMap(this::streamKeys)
            .collect(Collectors.toSet());

        if (allKeys.isEmpty()) {
            return Optional.empty();
        }

        // now we have all the keys, let's load them from the lazy sources
        for (RuntimeWithData data : loadedData) {
            if (data.runtime().isLazy()) {
                data.data(loadLazy(data.runtime(), allKeys));
            }
        }

        List<ObjectNode> objectNodes = loadedData.stream()
            .map(RuntimeWithData::data)
            .flatMap(Optional::stream)
            .collect(Collectors.toList());

        return Optional.of(mergingStrategy.merge(objectNodes));
    }

    private Optional<ObjectNode> loadLazy(ConfigSourceRuntimeImpl runtime, Set<KeyImpl> allKeys) {
        Map<String, ConfigNode> nodes = new HashMap<>();
        for (KeyImpl key : allKeys) {
            String stringKey = key.toString();
            runtime.node(stringKey).ifPresent(it -> nodes.put(stringKey, it));
        }

        if (nodes.isEmpty()) {
            return Optional.empty();
        }

        ObjectNodeImpl.Builder builder = ObjectNodeImpl.builder("");

        nodes.forEach(builder::addNode);

        return Optional.of(builder.build());
    }

    private Stream<KeyImpl> streamKeys(ObjectNode objectNode) {
        return ConfigUtils.createFullKeyToNodeMap(objectNode)
            .keySet()
            .stream();
    }

    public Iterable<ConfigSource> configSources() {
        return configSources;
    }

    private static final class RuntimeWithData {
        private final ConfigSourceRuntimeImpl runtime;
        private volatile Optional<ObjectNode> data;

        private RuntimeWithData(ConfigSourceRuntimeImpl runtime, Optional<ObjectNode> data) {
            this.runtime = runtime;
            this.data = data;
        }

        private void data(Optional<ObjectNode> data) {
            this.data = data;
        }

        private ConfigSourceRuntimeImpl runtime() {
            return runtime;
        }

        private Optional<ObjectNode> data() {
            return data;
        }

        @Override
        public String toString() {
            return runtime.toString();
        }
    }
}

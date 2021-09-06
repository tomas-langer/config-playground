package jakarta.config.impl;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import jakarta.config.spi.ConfigNode;
import jakarta.config.spi.ConfigSource;

class ObjectNodeImpl
    extends AbstractMap<String, ConfigNode> implements ConfigNode.ObjectNode {

    private final Map<String, ConfigNode> members;
    private final Function<String, String> resolveTokenFunction;
    private final String value;
    private final String key;
    private String description;
    private ConfigSourceInfo info = new ConfigSourceInfo();

    ObjectNodeImpl(Builder builder) {
        this.members = Map.copyOf(builder.members);
        this.resolveTokenFunction = builder.tokenResolver();
        this.value = builder.value;
        this.key = builder.key();
    }

    static Builder builder(String key) {
        return builder(key, Function.identity());
    }

    static Builder builder(String key, Function<String, String> tokenResolver) {
        return new Builder(key, tokenResolver);
    }

    /**
     * Wraps value node into mergeable value node.
     *
     * @param objectNode original node
     * @return new instance of mergeable node or original node if already was mergeable.
     */
    public static ObjectNodeImpl wrap(ObjectNode objectNode) {
        return wrap(objectNode, Function.identity());
    }

    /**
     * Wraps value node into mergeable value node.
     *
     * @param objectNode           original node
     * @param resolveTokenFunction a token resolver
     * @return new instance of mergeable node or original node if already was mergeable.
     */
    public static ObjectNodeImpl wrap(ObjectNode objectNode, Function<String, String> resolveTokenFunction) {
        return ObjectNodeImpl.builder(objectNode.key(), resolveTokenFunction)
            .members(objectNode)
            .update(it -> objectNode.value().ifPresent(it::value))
            .build();
    }

    private static String encodeDotsInTokenReferences(String key) {
        return key.replaceAll("\\.+(?=[^(\\$\\{)]*\\})", "~1");
    }

    @Override
    public Set<Entry<String, ConfigNode>> entrySet() {
        return members.entrySet();
    }

    @Override
    public ObjectNode merge(ConfigNode node) {
        switch (node.nodeType()) {
        case OBJECT:
            return mergeWithObjectNode((ObjectNodeImpl) node);
        case LIST:
            return mergeWithListNode((ListNodeImpl) node);
        case VALUE:
            return mergeWithValueNode((ValueNodeImpl) node);
        default:
            throw new IllegalArgumentException("Unsupported node type: " + node.getClass().getName());
        }
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public Optional<ConfigSource> configSource() {
        return info.configSource();
    }

    @Override
    public Optional<Integer> sourcePriority() {
        return info.sourcePriority();
    }

    @Override
    public void configSource(ConfigSource source) {
        info.configSource(source);
        members.forEach((key, value) -> value.configSource(source));
    }

    @Override
    public void sourcePriority(int priority) {
        info.sourcePriority(priority);
        members.forEach((key, value) -> value.sourcePriority(priority));
    }

    private ObjectNode mergeWithValueNode(ValueNodeImpl node) {
        return ObjectNodeImpl.builder(key(), resolveTokenFunction)
            .members(members)
            .update(it -> node.value().ifPresent(it::value))
            .build();
    }

    private ObjectNode mergeWithObjectNode(ObjectNodeImpl node) {
        //merge object 'node' with object this object members
        return ObjectNodeImpl.builder(key(), resolveTokenFunction)
            .members(members)
            .update(it -> {
                node.forEach((name, member) -> it.deepMerge(AbstractNodeBuilder.MergingKey.of(name),
                                                            AbstractNodeBuilder.wrap(member)));
                node.value().or(this::value).ifPresent(it::value);
            })
            .build();
    }

    private ObjectNode mergeWithListNode(ListNodeImpl node) {
        return ObjectNodeImpl.builder(key(), resolveTokenFunction)
            .members(members)
            .update(it -> {
                node.value().or(this::value).ifPresent(it::value);
                AtomicInteger index = new AtomicInteger(0);
                node.forEach(configNode -> {
                    int i = index.getAndIncrement();
                    it.merge(String.valueOf(i), configNode);
                });
            })
            .build();
    }

    @Override
    public String toString() {
        if (null == value) {
            return "ObjectNode[" + members.size() + "]" + super.toString();
        }
        return "ObjectNode(\"" + value + "\")[" + members.size() + "]" + super.toString();
    }

    /**
     * Description of this node.
     * @return node description
     */
    public String description() {
        return description;
    }

    @Override
    public boolean hasValue() {
        return null != value;
    }

    @Override
    public Optional<String> value() {
        return Optional.ofNullable(value);
    }

    static class Builder extends AbstractNodeBuilder<String, Builder>
            implements jakarta.common.Builder<Builder, ObjectNodeImpl> {

        private final Map<String, ConfigNode> members = new HashMap<>();
        private String value;

        private Builder(String key, Function<String, String> tokenResolver) {
            super(key, tokenResolver);
        }

        @Override
        public ObjectNodeImpl build() {
            return new ObjectNodeImpl(this);
        }

        public Builder addValue(String key, String value) {
            return deepMerge(MergingKey.of(encodeDotsInTokenReferences(tokenResolver().apply(key))),
                             ValueNodeImpl.create(key, value));
        }

        public Builder value(String value) {
            this.value = value;
            return this;
        }

        public Builder members(Map<String, ConfigNode> members) {
            members.forEach(this::addNode);
            return this;
        }

        public Builder addNode(String name, ConfigNode node) {
            members.put(tokenResolver().apply(name), wrap(node, tokenResolver()));
            return this;
        }

        @Override
        protected String id(MergingKey key) {
            return key.first();
        }

        @Override
        protected ConfigNode member(String name) {
            return members.computeIfAbsent(name, it -> ObjectNodeImpl.builder(key() + "." + name, tokenResolver()).build());
        }

        @Override
        protected void update(String name, ConfigNode node) {
            members.put(tokenResolver().apply(name), node);
        }

        @Override
        protected void merge(String name, ConfigNode node) {
            members.merge(name, node, ConfigNode::merge);
        }

        public Builder addList(String key, ListNode list) {
            return deepMerge(MergingKey.of(encodeDotsInTokenReferences(tokenResolver().apply(key))),
                             ListNodeImpl.wrap(list, tokenResolver()));
        }

        public Builder addObject(String key, ObjectNode object) {
            return deepMerge(MergingKey.of(encodeDotsInTokenReferences(tokenResolver().apply(key))),
                             ObjectNodeImpl.wrap(object, tokenResolver()));
        }
    }
}

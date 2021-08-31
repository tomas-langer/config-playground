package jakarta.config.impl;

import java.util.Optional;

import jakarta.config.spi.ConfigNode;

class ValueNodeImpl implements ConfigNode.ValueNode {

    private final String key;
    private final String value;

    private ValueNodeImpl(String key, String value) {
        this.key = key;
        this.value = value;
    }

    static ValueNodeImpl create(String key, String value) {
        return new ValueNodeImpl(key, value);
    }

    @Override
    public Optional<String> value() {
        return Optional.of(value);
    }

    @Override
    public String get() {
        return value;
    }

    @Override
    public ConfigNode merge(ConfigNode node) {
        switch (node.nodeType()) {
        case OBJECT:
            return mergeWithObjectNode((ObjectNodeImpl) node);
        case LIST:
            return mergeWithListNode((ListNodeImpl) node);
        case VALUE:
            return node;
        default:
            throw new IllegalArgumentException("Unsupported node type: " + node.getClass().getName());
        }
    }
    private ConfigNode mergeWithListNode(ListNodeImpl node) {
        if (node.hasValue()) {
            // will not merge, as the new node has priority over this node and we only have a value
            return node;
        }

        // and this will work fine, as the list node does not have a value, so we just add a value from this node
        return node.merge(this);
    }

    private ConfigNode mergeWithObjectNode(ObjectNodeImpl node) {
        // merge this value node with an object node
        ObjectNodeImpl.Builder builder = ObjectNodeImpl.builder(key());

        node.forEach((name, member) -> builder
            .deepMerge(AbstractNodeBuilder.MergingKey.of(name), AbstractNodeBuilder.wrap(member)));

        node.value().or(this::value).ifPresent(builder::value);

        return builder.build();
    }
    @Override
    public boolean hasValue() {
        return false;
    }

    @Override
    public String toString() {
        return key + '=' + value;
    }

    public String key() {
        return key;
    }

    /**
     * Wraps value node into mergeable value node.
     *
     * @param valueNode original node
     * @return new instance of mergeable node or original node if already was mergeable.
     */
    static ValueNodeImpl wrap(ValueNode valueNode) {
        if (valueNode instanceof ValueNodeImpl) {
            return (ValueNodeImpl) valueNode;
        }
        return ValueNodeImpl.create(valueNode.key(), valueNode.get());
    }

}

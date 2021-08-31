package jakarta.config.impl;

import java.util.AbstractList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import jakarta.config.spi.ConfigNode;
import jakarta.config.spi.ConfigNode.ListNode;

import static jakarta.config.impl.AbstractNodeBuilder.formatFrom;

class ListNodeImpl extends AbstractList<ConfigNode> implements ListNode {
    private final List<ConfigNode> elements;
    private final String value;
    private final String key;

    private String description;

    public ListNodeImpl(Builder builder) {
        this.elements = List.copyOf(builder.elements);
        this.value = builder.value;
        this.key = builder.key();
    }

    static Builder builder(String key) {
        return new Builder(key);
    }

    /**
     * Wraps list node into mergeable list node.
     *
     * @param listNode original node
     * @return new instance of mergeable node or original node if already was mergeable.
     */
    static ListNodeImpl wrap(ListNode listNode, Function<String, String> resolveTokenFunction) {
        if (listNode instanceof ListNodeImpl) {
            return (ListNodeImpl) listNode;
        }
        return builder(listNode.key())
            .from(listNode)
            .update(it -> listNode.value().ifPresent(it::value))
            .build();
    }

    static Builder builder(String key, Function<String, String> tokenResolver) {
        return new Builder(key, tokenResolver);
    }

    @Override
    public Optional<String> value() {
        return Optional.ofNullable(value);
    }

    @Override
    public ConfigNode merge(ConfigNode node) {
        switch (node.nodeType()) {
        case OBJECT:
            return mergeWithObject((ObjectNodeImpl) node);
        case LIST:
            return mergeWithList((ListNodeImpl) node);
        case VALUE:
            return builder(key())
                .from(this)
                .value(((ValueNodeImpl) node).get())
                .build();
        default:
            throw new IllegalArgumentException("Unsupported node type: " + node.getClass().getName());
        }
    }

    @Override
    public boolean hasValue() {
        return value != null;
    }

    @Override
    public ConfigNode get(int index) {
        return elements.get(index);
    }

    @Override
    public int size() {
        return elements.size();
    }

    @Override
    public String key() {
        return key;
    }

    private ConfigNode mergeWithList(ListNodeImpl node) {
        if (node.hasValue()) {
            return node;
        }

        if (hasValue()) {
            return builder(key())
                .from(node)
                .value(value)
                .build();
        }

        return node;
    }

    /**
     * Merges list with specified object.
     *
     * @param node object node to be merged with
     * @return new instance of node that contains merged list and object
     */
    private ConfigNode mergeWithObject(ObjectNodeImpl node) {
        Set<String> unprocessedPeerNames = new HashSet<>(node.keySet());

        final Builder builder = builder(key());

        node.value()
            .or(() -> Optional.ofNullable(value))
            .ifPresent(builder::value);

        for (int i = 0; i < elements.size(); i++) {
            ConfigNode element = elements.get(i);
            String name = String.valueOf(i);
            if (unprocessedPeerNames.contains(name)) {
                unprocessedPeerNames.remove(name);
                element = element.merge(node.get(name));
            }
            builder.addNode(element);
        }
        if (!unprocessedPeerNames.isEmpty()) {
            throw new IllegalStateException(
                String.format("Cannot merge OBJECT members %s%s with an LIST node%s.",
                              unprocessedPeerNames,
                              formatFrom(node.description()),
                              formatFrom(description)));
        } else {
            return builder.build();
        }
    }

    static class Builder extends AbstractNodeBuilder<Integer, Builder>
        implements jakarta.config.impl.Builder<Builder, ListNodeImpl> {
        private final List<ConfigNode> elements = new LinkedList<>();
        private String value;

        private Builder(String key) {
            this(key, Function.identity());
        }

        private Builder(String key, Function<String, String> tokenResolver) {
            super(key, tokenResolver);
        }

        @Override
        public ListNodeImpl build() {
            return new ListNodeImpl(this);
        }

        /**
         * Adds new element into the list.
         *
         * @param node new node
         * @return modified builder
         */
        Builder addNode(ConfigNode node) {
            elements.add(wrap(node));
            return this;
        }

        Builder value(String value) {
            this.value = value;
            return this;
        }

        @Override
        protected Integer id(MergingKey key) {
            String name = key.first();
            try {
                int index = Integer.parseInt(name);
                if (index < 0) {
                    throw new IllegalStateException("Cannot merge an OBJECT member '" + name
                                                        + "' into a LIST element. Illegal negative index " + index + ".");
                }
                if (index >= elements.size()) {
                    throw new IllegalStateException("Cannot merge an OBJECT member '" + name + "' into a LIST element. "
                                                        + "Index " + index + " out of bounds <0, " + (elements.size() - 1) + ">"
                                                        + ".");
                }
                return index;
            } catch (NumberFormatException ex) {
                throw new IllegalStateException("Cannot merge an OBJECT member '" + name + "' into a LIST element, not a number.",
                                                ex);
            }
        }

        @Override
        protected ConfigNode member(Integer index) {
            return elements.get(index);
        }

        @Override
        protected void update(Integer index, ConfigNode node) {
            elements.set(index, wrap(node));
        }

        @Override
        protected void merge(Integer index, ConfigNode node) {
            elements.set(index, elements.get(index).merge(node));
        }

        Builder addValue(ValueNode value) {
            return addNode(value);
        }

        Builder addObject(ObjectNode object) {
            return addNode(object);
        }

        Builder addList(ListNode list) {
            return addNode(list);
        }

        Builder from(ListNode listNode) {
            listNode.forEach(this::addNode);
            return this;
        }
    }
}

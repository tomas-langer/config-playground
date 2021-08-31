/*
 * Copyright (c) 2017, 2021 Oracle and/or its affiliates.
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

package jakarta.config.spi;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Marker interface identifying a config node implementation.
 */
public interface ConfigNode {
    /**
     * Key of this config node.
     *
     * @return key of this node
     */
    String key();

    /**
     * Get the type of this node.
     *
     * @return NodeType this node represents
     */
    NodeType nodeType();

    /**
     * Get the direct value of this config node. Any node type can have a direct value.
     *
     * @return a value if present, {@code empty} otherwise
     */
    Optional<String> value();

    /**
     * Returns new instance mergeable node of same type of original one that merges also with specified node.
     *
     * @param node node to be used to merge with
     * @return new instance of mergeable node that combines original node with specified one
     */
    ConfigNode merge(ConfigNode node);


    /**
     * Each node may have a direct value, and in addition may be an object node or a list node.
     * This method returns true for any node with direct value.
     *
     * @return true if this node contains a value
     */
    default boolean hasValue() {
        return value().isPresent();
    }

    /**
     * Config source that provided this value. Only available for nodes that are backed
     * by config source data.
     *
     * @return config source if available
     */
    Optional<ConfigSource> configSource();

    /**
     * The actual priority of the config source that provided this value.
     *
     * @return config source priority if available
     * @see #configSource()
     */
    Optional<Integer> sourcePriority();

    void configSource(ConfigSource source);

    void sourcePriority(int priority);

    /**
     * Base types of config nodes.
     */
    enum NodeType {
        /**
         * An object (complex structure), optionally may have a value.
         */
        OBJECT,
        /**
         * A list of values, optionally may have a value.
         */
        LIST,
        /**
         * Only has value.
         */
        VALUE
    }

    /**
     * Single string-based configuration value.
     */
    interface ValueNode extends ConfigNode {
        @Override
        default NodeType nodeType() {
            return NodeType.VALUE;
        }

        /**
         * Get the value of this value node.
         * @return string with the node value
         */
        String get();
    }

    /**
     * ConfigNode-based list of configuration values.
     * <p>
     * List may contains instances of
     *  {@link jakarta.config.spi.ConfigNode.ValueNode}, {@link jakarta.config.spi.ConfigNode.ListNode} as well as {@link jakarta.config.spi.ConfigNode.ObjectNode}.
     */
    interface ListNode extends ConfigNode, List<ConfigNode> {
        @Override
        default NodeType nodeType() {
            return NodeType.LIST;
        }
    }

    /**
     * Configuration node representing a hierarchical structure parsed by a
     * suitable {@link ConfigParser} if necessary.
     * <p>
     * In the map exposed by this interface, the map keys are {@code String}s
     * containing the fully-qualified dotted names of the config keys and the
     * map values are the corresponding {@link jakarta.config.spi.ConfigNode.ValueNode} or {@link jakarta.config.spi.ConfigNode.ListNode}
     * instances. The map never contains {@link jakarta.config.spi.ConfigNode.ObjectNode} values because the
     * {@link jakarta.config.spi.ConfigNode.ObjectNode} is implemented as a flat map.
     */
    interface ObjectNode extends ConfigNode, Map<String, ConfigNode> {
        @Override
        default NodeType nodeType() {
            return NodeType.OBJECT;
        }

        @Override
        ObjectNode merge(ConfigNode node);

    }
}

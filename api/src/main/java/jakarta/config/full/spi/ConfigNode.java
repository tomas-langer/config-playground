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

package jakarta.config.full.spi;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Marker interface identifying a config node implementation.
 */
public interface ConfigNode {
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
     *  {@link jakarta.config.full.spi.ConfigNode.ValueNode}, {@link jakarta.config.full.spi.ConfigNode.ListNode} as well as {@link jakarta.config.full.spi.ConfigNode.ObjectNode}.
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
     * map values are the corresponding {@link jakarta.config.full.spi.ConfigNode.ValueNode} or {@link jakarta.config.full.spi.ConfigNode.ListNode}
     * instances. The map never contains {@link jakarta.config.full.spi.ConfigNode.ObjectNode} values because the
     * {@link jakarta.config.full.spi.ConfigNode.ObjectNode} is implemented as a flat map.
     */
    interface ObjectNode extends ConfigNode, Map<String, ConfigNode> {
        @Override
        default NodeType nodeType() {
            return NodeType.OBJECT;
        }
    }
}

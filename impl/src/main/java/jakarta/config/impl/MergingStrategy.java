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

import java.util.List;

import jakarta.config.spi.ConfigNode.ObjectNode;

/**
 * An algorithm for combining multiple {@code ConfigNode.ObjectNode} root nodes
 * into a single {@code ConfigNode.ObjectNode} root node.
 *
 * @see #fallback() default merging strategy
 */
interface MergingStrategy {
    /**
     * Merges an ordered list of {@link io.helidon.config.spi.ConfigNode.ObjectNode}s into a
     * single instance.
     * <p>
     * Typically nodes (object, list or value) from a root earlier in the
     * list are considered to have a higher priority than nodes from a root
     * that appears later in the list, but this is not required and is
     * entirely up to each {@code MergingStrategy} implementation.
     *
     * @param rootNodes list of root nodes to combine
     * @return ObjectNode root node resulting from the merge
     */
    ObjectNode merge(List<ObjectNode> rootNodes);
}

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

package jakarta.config.tree.spi;

/**
 * An eager source that can read all data from the underlying origin as a configuration node.
 */
public interface NodeConfigSource extends ConfigSource {
    /**
     * Load the underlying configuration data as a tree.
     * This approach expects immutable configuration.
     *
     * @return loaded root node
     */
    ConfigNode.ObjectNode load();
}

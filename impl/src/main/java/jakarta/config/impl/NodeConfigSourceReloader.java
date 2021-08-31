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

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import jakarta.config.spi.ConfigNode;
import jakarta.config.spi.NodeConfigSource;

class NodeConfigSourceReloader implements Supplier<Optional<ConfigNode.ObjectNode>> {
    private final NodeConfigSource configSource;
    private final AtomicReference<Object> lastStamp;
    private final Supplier<Integer> sourcePriority;

    NodeConfigSourceReloader(NodeConfigSource configSource,
                             AtomicReference<Object> lastStamp,
                             Supplier<Integer> sourcePriority) {
        this.configSource = configSource;
        this.lastStamp = lastStamp;
        this.sourcePriority = sourcePriority;
    }

    @Override
    public Optional<ConfigNode.ObjectNode> get() {
        return configSource.load()
            .map(content -> {
                lastStamp.set(content.stamp().orElse(null));
                ConfigNode.ObjectNode data = content.data();
                data.configSource(configSource);
                data.sourcePriority(sourcePriority.get());
                return data;
            });
    }
}

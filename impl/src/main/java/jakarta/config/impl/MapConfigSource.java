/*
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.
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

import java.util.Map;
import java.util.Optional;

import jakarta.config.spi.ConfigContent;
import jakarta.config.spi.NodeConfigSource;
import jakarta.config.spi.PollableConfigSource;

/**
 * Map based config source.
 */
class MapConfigSource implements NodeConfigSource, PollableConfigSource<Map<String, String>> {
    private final Map<String, String> map;
    private final String name;

    MapConfigSource(String name, Map<String, String> map) {
        this.name = name;
        this.map = map;
    }

    @Override
    public Optional<ConfigContent.NodeContent> load() {
        return Optional.of(new NodeContentImpl(ConfigUtils.mapToObjectNode(map),
                                               Map.copyOf(map)));
    }

    @Override
    public boolean isModified(Map<String, String> stamp) {
        return !stamp.equals(map);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return getName() + " (" + getPriority() + ")";
    }
}

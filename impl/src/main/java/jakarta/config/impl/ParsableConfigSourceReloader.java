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
import jakarta.config.spi.ConfigParser;
import jakarta.config.spi.ParsableConfigSource;

class ParsableConfigSourceReloader implements Supplier<Optional<ConfigNode.ObjectNode>> {
    private final ParsableConfigSource configSource;
    private final AtomicReference<Object> lastStamp;
    private final Supplier<Integer> sourcePriority;
    private final ConfigSourceContextImpl context;

    ParsableConfigSourceReloader(ConfigSourceContextImpl context,
                                 ParsableConfigSource configSource,
                                 AtomicReference<Object> lastStamp,
                                 Supplier<Integer> sourcePriority) {
        this.context = context;
        this.configSource = configSource;
        this.lastStamp = lastStamp;
        // must be a supplier, as this may be only available after initial load
        this.sourcePriority = sourcePriority;
    }

    @Override
    public Optional<ConfigNode.ObjectNode> get() {
        return configSource.load()
            .map(content -> {
                lastStamp.set(content.stamp().orElse(null));
                Optional<ConfigParser> parser = configSource.parser();

                if (parser.isPresent()) {
                    ConfigNode.ObjectNode parsed = parser.get().parse(content);
                    parsed.configSource(configSource);
                    parsed.sourcePriority(sourcePriority.get());
                    return parsed;
                }

                // media type should either be configured on config configSource, or in content
                Optional<String> mediaType = configSource.mediaType().or(content::mediaType);

                if (mediaType.isPresent()) {
                    parser = context.findParser(mediaType.get());
                    if (parser.isEmpty()) {
                        throw new IllegalStateException("Cannot find suitable parser for '" + mediaType
                            .get() + "' media type for config configSource " + configSource.getName());
                    }
                    ConfigNode.ObjectNode parsed = parser.get().parse(content);
                    parsed.configSource(configSource);
                    parsed.sourcePriority(sourcePriority.get());
                    return parsed;
                }

                throw new IllegalStateException("Could not find media type of config configSource " + configSource.getName());
            });
    }
}

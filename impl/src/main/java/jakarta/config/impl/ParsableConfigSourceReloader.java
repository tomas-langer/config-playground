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
    private final ConfigSourceContextImpl context;

    ParsableConfigSourceReloader(ConfigSourceContextImpl context, ParsableConfigSource configSource,
                                 AtomicReference<Object> lastStamp) {
        this.context = context;
        this.configSource = configSource;
        this.lastStamp = lastStamp;
    }

    @Override
    public Optional<ConfigNode.ObjectNode> get() {
        return configSource.load()
            .map(content -> {
                lastStamp.set(content.stamp().orElse(null));
                Optional<ConfigParser> parser = configSource.parser();

                if (parser.isPresent()) {
                    return parser.get().parse(content);
                }

                // media type should either be configured on config source, or in content
                Optional<String> mediaType = configSource.mediaType().or(content::mediaType);

                if (mediaType.isPresent()) {
                    parser = context.findParser(mediaType.get());
                    if (parser.isEmpty()) {
                        throw new IllegalStateException("Cannot find suitable parser for '" + mediaType
                            .get() + "' media type for config source " + configSource.getName());
                    }
                    return parser.get().parse(content);
                }

                throw new IllegalStateException("Could not find media type of config source " + configSource.getName());
            });
    }
}

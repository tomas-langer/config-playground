/*
 * Copyright (c) 2017, 2020 Oracle and/or its affiliates.
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

import java.util.Optional;

/**
 * Context created by a {@link io.helidon.config.Config.Builder} as it constructs a
 * {@link io.helidon.config.Config}.
 * <p>
 * The context is typically used in implementations of {@link io.helidon.config.spi}
 * interfaces to share common information.
 */
public interface ConfigSourceContext {
    /**
     * Create or find a runtime for a config source.
     *
     * @param source source to create runtime for
     * @return a source runtime
     */
    ConfigSourceRuntime sourceRuntime(ConfigSource source);

    Optional<ConfigParser> findParser(String mediaType);
}

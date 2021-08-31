/*
 * Copyright (c) 2009-2020 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *   2009       - Mark Struberg
 *      Ordinal solution in Apache OpenWebBeans
 *   2011-12-28 - Mark Struberg & Gerhard Petracek
 *      Contributed to Apache DeltaSpike fb0131106481f0b9a8fd
 *   2016-11-14 - Emily Jiang / IBM Corp
 *      Methods renamed, JavaDoc and cleanup
 */
package jakarta.config.spi;

public interface ConfigSource {
    /**
     * The default configuration priority value, {@value}.
     */
    int DEFAULT_PRIORITY = 100;

    String getName();

    /**
     * If the underlying data exist at this time.
     * This is to prevent us loading such a source if we know it does not exist.
     *
     * @return {@code true} if the source exists, {@code false} otherwise
     */
    default boolean exists() {
        return true;
    }

    /**
     * Whether this source is optional.
     *
     * @return return {@code true} for optional source, returns {@code false} by default
     */
    default boolean optional() {
        return false;
    }

    default int getPriority() {
        return DEFAULT_PRIORITY;
    }

    /**
     * Initialize the config source with a {@link ConfigSourceContext}.
     * <p>
     * The method is executed during {@link Config} bootstrapping by {@link Config.Builder}.
     *
     * @param context a config context
     */
    default void init(ConfigSourceContext context) {
    }
}

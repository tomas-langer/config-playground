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

package jakarta.config.spi;

import java.util.Optional;

/**
 * An eager source that can read all data from the underlying origin as a stream that can be
 * parsed based on its media type (or using an explicit {@link ConfigParser}.
 */
public interface ParsableConfigSource extends ConfigSource {
    /**
     * Loads the underlying source data. This method is only called when the source {@link #exists()}.
     * <p>
     * The method can be invoked repeatedly, for example during retries.
     * In case the underlying data is gone or does not exist, return an empty optional.
     *
     * @return The parsable content to be processed by a parser (if present)
     */
    Optional<ConfigContent.ParsableContent> load();

    /**
     * If a parser is configured with this source, return it.
     * The source implementation does not need to handle config parser.
     *
     * @return content parser if one is configured on this source
     */
    Optional<ConfigParser> parser();

    /**
     * If media type is configured on this source, or can be guessed from the underlying origin, return it.
     * The media type may be used to locate a {@link ConfigParser} if one is not explicitly
     * configured.
     *
     * @return media type if configured or detected from content
     */
    Optional<String> mediaType();
}

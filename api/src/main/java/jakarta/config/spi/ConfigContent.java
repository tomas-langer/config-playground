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

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Optional;

/**
 * Config content as provided by a config source that can read all its data at once (an "eager" config source).
 * This interface provides necessary support for changes of the underlying source and for parsable content.
 * <p>
 * Content can either provide a {@link ConfigNode.ObjectNode} or an array of bytes to be parsed by a
 * {@link ConfigParser}.
 * <p>
 * The data stamp can be any object (to be decided by the {@link ConfigSource}).
 */
public interface ConfigContent {
    /**
     * Close the content, as it is no longer needed.
     */
    default void close() {
    }

    /**
     * A modification stamp of the content.
     * <p>
     * @return a stamp of the content
     */
    default Optional<Object> stamp() {
        return Optional.empty();
    }

    /**
     * Config content that provides an {@link ConfigNode.ObjectNode} directly, with no need
     * for parsing.
     */
    interface NodeContent extends ConfigContent {
        /**
         * Data of this config source.
         * @return the data of the underlying source as an object node
         */
        ConfigNode.ObjectNode data();
    }

    interface ParsableContent extends ConfigContent {
        /**
         * Media type of the content. This method is only called if
         * there is no parser configured.
         *
         * @return content media type if known, {@code empty} otherwise
         */
        Optional<String> mediaType();

        /**
         * Data of this config source.
         *
         * @return the data of the underlying source to be parsed by a {@link ConfigParser}
         */
        InputStream data();

        /**
         * Charset configured by the config source or {@code UTF-8} if none configured.
         *
         * @return charset to use when reading {@link #data()} if needed by the parser
         */
        Charset charset();
    }
}

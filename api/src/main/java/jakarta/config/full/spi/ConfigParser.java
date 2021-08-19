/*
 * Copyright (c) 2017, 2021 Oracle and/or its affiliates.
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

package jakarta.config.full.spi;

import java.util.List;
import java.util.Set;

import jakarta.config.full.spi.ConfigContent.ParsableContent;
import jakarta.config.full.spi.ConfigNode.ObjectNode;

/**
 * Transforms config {@link io.helidon.config.spi.ConfigParser.Content} into a {@link ObjectNode} that
 * represents the original structure and values from the content.
 * <p>
 * The application can register parsers on a {@code Builder} using the
 * {@link io.helidon.config.Config.Builder#addParser(io.helidon.config.spi.ConfigParser)} method. The
 * config system also locates parsers using the Java
 * {@link java.util.ServiceLoader} mechanism and automatically adds them to
 * every {@code Builder} unless the application disables this feature for a
 * given {@code Builder} by invoking
 * {@link io.helidon.config.Config.Builder#disableParserServices()}.
 * <p>
 * A parser can specify a {@link javax.annotation.Priority}. If no priority is
 * explicitly assigned, the value of {@value PRIORITY} is assumed.
 * <p>
 * Parser is used by the config system and a config source provides data as an input stream.
 *
 * @see io.helidon.config.Config.Builder#addParser(io.helidon.config.spi.ConfigParser)
 * @see io.helidon.config.spi.ParsableSource
 * @see ConfigParsers ConfigParsers - access built-in implementations.
 */
public interface ConfigParser {

    /**
     * Default priority of the parser if registered by {@link io.helidon.config.Config.Builder} automatically.
     */
    int PRIORITY = 100;

    /**
     * Returns set of supported media types by the parser.
     * <p>
     * Set of supported media types is used when config system looks for appropriate parser based on media type
     * of content.
     * <p>
     * {@link io.helidon.config.spi.ParsableSource} implementations can use {@link io.helidon.common.media.type.MediaTypes}
     * to probe for media type of content to provide it to config system through
     * {@link io.helidon.config.spi.ConfigParser.Content.Builder#mediaType(String)}.
     *
     * @return supported media types by the parser
     */
    Set<String> supportedMediaTypes();

    /**
     * Parses a specified {@link ConfigContent} into a {@link ObjectNode hierarchical configuration representation}.
     * <p>
     * Never returns {@code null}.
     *
     * @param content a content to be parsed
     * @return parsed hierarchical configuration representation
     */
    ObjectNode parse(ParsableContent content);

    /**
     * Config parser can define supported file suffixes. If such are defined, Helidon will
     * use these to discover default configuration sources.
     * For example if there is a {@code ConfigParser} that returns {@code xml}, config would look for
     * {@code meta-config.xml} to discover meta configuration, and for {@code application.xml} on file
     * system and on classpath to discover configuration files.
     * <p>
     * Note that the suffixes must resolve into a media type supported by a config parser
     * (see {@link io.helidon.common.media.type.MediaTypes#detectExtensionType(String)}).
     *
     * @return a set of file suffixes supported by this config parser.
     */
    default List<String> supportedSuffixes() {
        return List.of();
    }
}

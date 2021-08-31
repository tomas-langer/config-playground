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

package jakarta.config.impl;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.config.Config;
import jakarta.config.spi.ConfigNode;

/**
 * Implementation of {@link Config} that represents complex (object, list) nodes.
 */
abstract class ConfigComplexImpl<N extends ConfigNode> extends ConfigExistingImpl<N> {

    ConfigComplexImpl(Type type,
                      KeyImpl prefix,
                      KeyImpl key,
                      N node,
                      ConfigFactory factory,
                      ConversionSupport mapperManager) {
        super(type, prefix, key, node, factory, mapperManager);
    }

    @Override
    public <T> Optional<List<T>> asList(Class<T> type) {
        return asNodeList()
            .map(list -> list.stream()
                .map(theConfig -> theConfig.as(type).get())
                .collect(Collectors.toList()));
    }

    @Override
    public final Stream<Config> traverse(Predicate<Config> predicate) {
        return asNodeList()
            .map(list -> list.stream()
                .filter(predicate)
                .map(node -> traverseSubNodes(node, predicate))
                .reduce(Stream.empty(), Stream::concat))
            .orElseThrow(() -> new NoSuchElementException("Missing node " + key()));

    }

    private Stream<Config> traverseSubNodes(Config config, Predicate<Config> predicate) {
        if (config.type() == Type.VALUE) {
            return Stream.of(config);
        } else {
            return config.asNodeList()
                .map(list -> list.stream()
                    .filter(predicate)
                    .map(node -> traverseSubNodes(node, predicate))
                    .reduce(Stream.of(config), Stream::concat))
                .orElseThrow(() -> new NoSuchElementException("Missing node " + key()));
        }
    }

}

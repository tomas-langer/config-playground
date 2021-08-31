/*
 * Copyright (c) 2019, 2020 Oracle and/or its affiliates.
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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Media type detection based on file suffix.
 */
class MediaTypes {
    private static final String DEFAULT_MEDIA_TYPES = "default-media-types.properties";
    private static final String CUSTOM_MEDIA_TYPES = "META-INF/jakarta/config-media-types.properties";

    private static final Logger LOGGER = Logger.getLogger(MediaTypes.class.getName());
    private static final Map<String, String> MAPPINGS = new HashMap<>();
    private static final ConcurrentHashMap<String, Optional<String>> CACHE = new ConcurrentHashMap<>();

    static {
        // built-in
        try (InputStream builtIns = MediaTypes.class.getResourceAsStream(DEFAULT_MEDIA_TYPES)) {
            if (null != builtIns) {
                Properties properties = new Properties();
                properties.load(builtIns);
                for (String name : properties.stringPropertyNames()) {
                    MAPPINGS.put(name, properties.getProperty(name));
                }
            } else {
                LOGGER.log(Level.SEVERE, "Failed to find default media type mapping resource");
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load default media types mapping", e);
        }

        // look for configured mapping by a user
        // to override existing mappings from default
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        try {
            Enumeration<URL> resources = classLoader.getResources(CUSTOM_MEDIA_TYPES);
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                LOGGER.finest(() -> "Loading custom media type mapping from: " + url);
                try (InputStream is = url.openStream()) {
                    Properties properties = new Properties();
                    properties.load(is);
                    for (String name : properties.stringPropertyNames()) {
                        MAPPINGS.put(name, properties.getProperty(name));
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load custom media types mapping", e);
        }
    }

    private static Optional<String> detectExtensionType(String fileSuffix) {
        return Optional.ofNullable(MAPPINGS.get(fileSuffix));
    }

    static Optional<String> detectType(Path file) {
        Path fileName = file.getFileName();
        if (null == fileName) {
            return Optional.empty();
        }
        return detectType(fileName.toString());
    }

    static Optional<String> detectType(String fileName) {
        // file string - we are interested in last . index
        int index = fileName.lastIndexOf('.');

        String inProgress = fileName;
        if (index > -1) {
            inProgress = inProgress.substring(index + 1);
        } else {
            // there is no suffix
            return Optional.empty();
        }

        // and now it should be safe - just a suffix
        return detectExtensionType(inProgress);
    }

    static Optional<String> detectSuffix(String fileSuffix) {
        return CACHE.computeIfAbsent(fileSuffix, it -> detectExtensionType(fileSuffix));
    }
}

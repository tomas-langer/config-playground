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

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import jakarta.config.spi.ConfigSource;
import jakarta.config.spi.NodeConfigSource;
import jakarta.config.spi.ParsableConfigSource;

/**
 * Utilities for MicroProfile Config {@link jakarta.config.spi.ConfigSource}.
 * <p>
 * The following methods create MicroProfile config sources to help with manual setup of Config
 * from {@link jakarta.config.spi.ConfigProviderResolver#getBuilder()}:
 * <ul>
 *     <li>{@link #systemProperties()} - system properties config configSource</li>
 *     <li>{@link #environmentVariables()} - environment variables config configSource</li>
 *     <li>{@link #create(java.nio.file.Path)} - load a properties file from file system</li>
 *     <li>{@link #create(String, java.nio.file.Path)} - load a properties file from file system with custom name</li>
 *     <li>{@link #create(java.util.Map)} - create an in-memory configSource from map</li>
 *     <li>{@link #create(String, java.util.Map)} - create an in-memory configSource from map with custom name</li>
 *     <li>{@link #create(java.util.Properties)} - create an in-memory configSource from properties</li>
 *     <li>{@link #create(String, java.util.Properties)} - create an in-memory configSource from properties with custom name</li>
 * </ul>
 */
public final class ConfigSources {
    private ConfigSources() {
    }

    /**
     * In memory config configSource based on the provided map.
     *
     * @param name name of the configSource
     * @param theMap map serving as configuration data
     * @return a new config configSource
     */
    public static ConfigSource create(String name, Map<String, String> theMap) {
        return new MapConfigSource(name, theMap);
    }

    /**
     * In memory config configSource based on the provided map.
     *
     * @param theMap map serving as configuration data
     * @return a new config configSource
     */
    public static ConfigSource create(Map<String, String> theMap) {
        return create("Map", theMap);
    }

    /**
     * {@link java.util.Properties} config configSource based on a file on file system.
     * The file is read just once, when the configSource is created and further changes to the underlying file are
     * ignored.
     *
     * @param path path of the properties file on the file system
     * @return a new config configSource
     */
    public static ConfigSource create(Path path) {
        return create("File: " + path.toAbsolutePath(), path);
    }

    /**
     * {@link java.util.Properties} config configSource based on a URL.
     * The URL is read just once, when the configSource is created and further changes to the underlying resource are
     * ignored.
     *
     * @param url url of the properties file (any URL scheme supported by JVM can be used)
     * @return a new config configSource
     */
    public static ParsableConfigSource create(URL url) {
        String name = url.toString();

        return new UrlConfigSource(name, url);
    }

    /**
     * {@link java.util.Properties} config configSource based on a URL with a profile override.
     * The URL is read just once, when the configSource is created and further changes to the underlying resource are
     * ignored.
     *
     * @param url url of the properties file (any URL scheme supported by JVM can be used)
     * @param profileUrl url of the properties file of profile specific configuration
     * @return a new config configSource
     */
    public static ConfigSource create(URL url, URL profileUrl) {
        ParsableConfigSource defaultSource = create(url);
        ParsableConfigSource profileSource = create(profileUrl);

        return composite(profileSource, defaultSource);
    }

    /**
     * {@link java.util.Properties} config configSource based on a file on file system.
     * The file is read just once, when the configSource is created and further changes to the underlying file are
     * ignored.
     *
     * @param name name of the config configSource
     * @param path path of the properties file on the file system
     * @return a new config configSource
     */
    public static ParsableConfigSource create(String name, Path path) {
        return new FileConfigSource(name, path);
    }

    /**
     * In memory config configSource based on the provided properties.
     *
     * @param properties serving as configuration data
     * @return a new config configSource
     */
    public static ConfigSource create(Properties properties) {
        return create("Properties", properties);
    }

    /**
     * In memory config configSource based on the provided properties.
     *
     * @param name name of the config configSource
     * @param properties serving as configuration data
     * @return a new config configSource
     */
    public static NodeConfigSource create(String name, Properties properties) {
        Map<String, String> result = new HashMap<>();
        for (String key : properties.stringPropertyNames()) {
            result.put(key, properties.getProperty(key));
        }
        return new MapConfigSource(name, result);
    }

    /**
     * Environment variables config configSource.
     * This configSource takes care of replacement of properties by environment variables as defined
     * in MicroProfile Config specification.
     * This config configSource is immutable and caching.
     *
     * @return a new config configSource
     */
    public static ConfigSource environmentVariables() {
        return new EnvironmentVariablesConfigSource();
    }

    /**
     * In memory config configSource based on system properties.
     *
     * @return a new config configSource
     */
    public static ConfigSource systemProperties() {
        return new SystemPropertiesConfigSource();
    }

    /**
     * Find all resources on classpath and return a config configSource for each.
     * Order is kept as provided by class loader.
     *
     * @param resource resource to find
     * @return a config configSource for each resource on classpath, empty if none found
     */
    public static List<ConfigSource> classPath(String resource) {
        return classPath(Thread.currentThread().getContextClassLoader(), resource);
    }

    /**
     * Find all resources on classpath and return a config configSource for each.
     * Order is kept as provided by class loader.
     *
     * The profile will be used to locate a configSource with {@code -${profile}} name, such as
     * {@code microprofile-config-dev.properties} for dev profile.
     *
     * @param resource resource to find
     * @param profile configuration profile to use, must not be null
     * @return a config configSource for each resource on classpath, empty if none found
     */
    public static List<ConfigSource> classPath(String resource, String profile) {
        return classPath(Thread.currentThread().getContextClassLoader(), resource, profile);
    }

    /**
     * Find all resources on classpath and return a config configSource for each.
     * Order is kept as provided by class loader.
     *
     * @param classLoader class loader to use to locate the resources
     * @param resource resource to find
     * @return a config configSource for each resource on classpath, empty if none found
     */
    public static List<ConfigSource> classPath(ClassLoader classLoader, String resource) {
        List<ConfigSource> sources = new LinkedList<>();
        try {
            classLoader.getResources(resource)
                .asIterator()
                .forEachRemaining(it -> {
                    if ("file".equals(it.getProtocol())) {
                        Path path = Paths.get(it.getPath());
                        sources.add(create("Classpath: " + path.toAbsolutePath(), path));
                    } else {
                        sources.add(new UrlConfigSource("Classpath: " + it, it));
                    }
                });
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read \"" + resource + "\" from classpath", e);
        }

        return sources;
    }

    /**
     * Find all resources on classpath and return a config configSource for each with a profile.
     * Order is kept as provided by class loader.
     *
     * The profile will be used to locate a configSource with {@code -${profile}} name, such as
     * {@code microprofile-config-dev.properties} for dev profile.
     *
     * @param classLoader class loader to use to locate the resources
     * @param resource resource to find
     * @param profile configuration profile to use, must not be null
     * @return a config configSource for each resource on classpath, empty if none found
     */
    public static List<ConfigSource> classPath(ClassLoader classLoader, String resource, String profile) {
        Objects.requireNonNull(profile, "Profile must be defined");

        List<ConfigSource> sources = new LinkedList<>();

        try {
            Enumeration<URL> baseResources = classLoader.getResources(resource);
            Enumeration<URL> profileResources = classLoader.getResources(toProfileResource(resource, profile));

            if (profileResources.hasMoreElements()) {
                List<URL> profileResourceList = new LinkedList<>();
                profileResources.asIterator()
                        .forEachRemaining(profileResourceList::add);

                baseResources.asIterator()
                        .forEachRemaining(it -> {
                            String pathBase = pathBase(it.toString());
                            // we need to find profile that belongs to this
                            for (URL url : profileResourceList) {
                                String profilePathBase = pathBase(url.toString());
                                if (pathBase.equals(profilePathBase)) {
                                    sources.add(create(it, url));
                                } else {
                                    sources.add(create(it));
                                }
                            }
                        });
            } else {
                baseResources
                        .asIterator()
                        .forEachRemaining(it -> sources.add(create(it)));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read \"" + resource + "\" from classpath", e);
        }

        return sources;
    }

    /**
     * Create a composite config configSource that uses the main first, and if it does not find
     * a property in main, uses fallback. This is useful to set up a config configSource with a profile,
     * where the profile configSource is {@code main} and the non-profile configSource is {@code fallback}.
     *
     * @param main look for properties here first
     * @param fallback if not found in main, look here
     * @return a new config configSource
     */
    public static ConfigSource composite(ConfigSource main, ConfigSource fallback) {
        String name = main.getName() + " (" + fallback.getName() + ")";

        return new CompositeConfigSource(name, main, fallback);
    }

    private static String pathBase(String path) {
        int i = path.lastIndexOf('/');
        int y = path.lastIndexOf('!');
        int z = path.lastIndexOf(':');
        int b = path.lastIndexOf('\\');

        // we need the last index before the file name - so the highest number of all of the above
        int max = Math.max(i, y);
        max = Math.max(max, z);
        max = Math.max(max, b);

        if (max > -1) {
            return path.substring(0, max);
        }
        return path;
    }

    private static String toProfileResource(String resource, String profile) {
        int i = resource.lastIndexOf('.');
        if (i > -1) {
            return resource.substring(0, i) + "-" + profile + resource.substring(i);
        }
        return resource + "-" + profile;
    }
}

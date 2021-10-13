/*
 * Copyright (c) 2021 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
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

import java.util.Set;

import jakarta.annotation.Priority;
import jakarta.config.ReservedKeys;

/**
 * A <em>configuration source</em> which provides configuration values from a specific place.
 */
public interface ConfigSource {
    /**
     * Default priority of a config source.
     */
    int DEFAULT_PRIORITY = 1000;

    /**
     * The name of the configuration source. The name might be used for logging or for analysis of configured values,
     * and also may be used in ordering decisions.
     * <p>
     * An example of a configuration source name is "{@code property-file mylocation/myprops.properties}".
     *
     * @return the name of the configuration source
     */
    String getName();

    /**
     * Return priority of this config source.
     * Can be defined using a {@link jakarta.annotation.Priority}, may be overridden using
     * key {@link jakarta.config.ReservedKeys#SOURCE_PRIORITY}, or by implementing this method.
     *
     * @return priority of this config source
     */
    default int getPriority() {
        // use priority defined in config source data
        String priority = getValue(ReservedKeys.SOURCE_PRIORITY);
        if (priority != null) {
            try {
                return Integer.parseInt(priority);
            } catch (NumberFormatException ignored) {
            }
        }
        // use priority from annotation
        Priority annotation = getClass().getAnnotation(Priority.class);
        if (annotation != null) {
            return annotation.value();
        }

        // use default priority
        return DEFAULT_PRIORITY;
    }

    /**
     * Return the value for the specified key in this configuration source.
     *
     * @param key
     *            the property name
     * @return the property value, or {@code null} if the property is not present
     */
    String getValue(String key);

    /**
     * Gets all property names known to this configuration source, potentially without evaluating the values. The
     * returned property names may be a subset of the names of the total set of retrievable properties in this config
     * source.
     * <p>
     * The returned set is not required to allow concurrent or multi-threaded iteration; however, if the same set is
     * returned by multiple calls to this method, then the implementation must support concurrent and multi-threaded
     * iteration of that set.
     *
     * @return a set of property names that are known to this configuration source
     */
    Set<String> getKeys();
}

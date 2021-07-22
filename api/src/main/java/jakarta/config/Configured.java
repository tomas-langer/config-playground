/*
 * Copyright (c) 2016-2019 Contributors to the Eclipse Foundation
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
 */
package jakarta.config;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Configured method in a mapped interface.
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface Configured {
    String UNCONFIGURED_VALUE = "org.eclipse.microprofile.config.configproperty.unconfigureddvalue";

    /**
     * The key of the config property used to look up the configuration value.
     * <p>
     * If it is not specified, it will be derived automatically from the name of the method.
     *
     * @return Name (key) of the config property to inject
     */
    String value() default "";

    /**
     * The default value if the configured property does not exist.
     * <p>
     * If the target Type is not String, a proper {@link jakarta.config.spi.Converter} will get
     * applied.
     *
     * @return the default value as a string
     */
    String defaultValue() default UNCONFIGURED_VALUE;
}

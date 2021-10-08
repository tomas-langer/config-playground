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
package jakarta.config;

/**
 * Reserved keys that have meaning for the configuration itself.
 */
public final class ReservedKeys {
    private static final String CONFIG_PREFIX = "jakarta.config.";
    /**
     * The value of the property specifies a single active profile.
     */
    public static final String PROFILE = CONFIG_PREFIX + "profile";
    /**
     * The value of the property determines whether the property expression is enabled or disabled. The value
     * <code>false</code> means the property expression is disabled, while <code>true</code> means enabled.
     *
     * By default, the value is set to <code>true</code>.
     */
    public static final String VALUE_EXPRESSIONS_ENABLED = CONFIG_PREFIX + "value.expressions.enabled";
    /**
     * The value of the property determines whether key expressions are enabled or disabled.
     * Not used.
     */
    public static final String KEY_EXPRESSIONS_ENABLED = CONFIG_PREFIX + "key.expressions.enabled";
    /**
     * Priority of the config source.
     * This property has no meaning when requested from config, only used for ordering of sources before
     * config is created.
     */
    public static final String SOURCE_PRIORITY = CONFIG_PREFIX + "source.priority";
    /**
     * If we significantly change the way source is loaded, we may have a possibility to
     * load it in the old approach.
     */
    public static final String SOURCE_VERSION = CONFIG_PREFIX + "source.version";

    private ReservedKeys() {
    }
}

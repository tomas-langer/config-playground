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

package jakarta.config.spi;

import java.time.Instant;

/**
 * Mechanism for notifying interested listeners when they should check for
 * changes that might have been made to the data used to create a {@code Config}
 * tree, as accessed through {@link jakarta.config.spi.PollableConfigSource}s.
 */
@FunctionalInterface
public interface PollingStrategy {

    /**
     * Start this polling strategy. From this point in time, the polled will receive
     *  events on {@link jakarta.config.spi.PollingStrategy.Polled#poll(java.time.Instant)}.
     * It is the responsibility of the {@link PollingStrategy.Polled}
     * to handle such requests.
     * A {@link ConfigSource} needs only support for polling stamps
     * to support a polling strategy, the actual reloading is handled by the
     * configuration component.
     * There is no need to implement {@link PollingStrategy.Polled} yourself,
     * unless you want to implement a new component that supports polling.
     * Possible reloads of configuration are happening within the thread that invokes this method.
     *
     * @param polled a component receiving polling events.
     */
    void start(Polled polled);

    /**
     * Stop polling and release all resources.
     */
    default void stop() {
    }

    /**
     * A polled component. For config this interface is implemented by the config system itself.
     */
    @FunctionalInterface
    interface Polled {
        /**
         * Poll for changes.
         * The result may be used to modify behavior of the {@link jakarta.config.spi.PollingStrategy} triggering this
         * poll event.
         *
         * @param when instant this polling request was created
         * @return result of the polling
         */
        ChangeEventType poll(Instant when);
    }

}

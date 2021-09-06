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
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.config.spi.ChangeEventType;
import jakarta.config.spi.ChangeWatcher;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

/**
 * This change watcher is backed by {@link java.nio.file.WatchService} to fire a polling event with every change on monitored
 * {@link java.nio.file.Path}.
 * <p>
 * When a parent directory of the {@code path} is not available, or becomes unavailable later, a new attempt to register {@code
 * WatchService} is scheduled again and again until the directory finally exists and the registration is successful.
 * <p>
 * This {@link ChangeWatcher} must be initialized with a custom {@link java.util.concurrent.ScheduledExecutorService executor}.
 * <p>
 * This watcher notifies with appropriate change event in the following cases:
 * <ul>
 *     <li>The watched directory is gone</li>
 *     <li>The watched directory appears</li>
 *     <li>A file in the watched directory is deleted, created or modified</li>
 * </ul>
 * <p>
 * A single file system watcher may be used to watch multiple targets. In such a case, if {@link #stop()} is invoked, it stops
 * watching all of these targets.
 *
 * @see java.nio.file.WatchService
 */
public final class FileChangeWatcher implements ChangeWatcher<Path> {

    private static final Logger LOGGER = Logger.getLogger(FileChangeWatcher.class.getName());

    /*
     * Configurable options through builder.
     */
    private final List<WatchEvent.Modifier> watchServiceModifiers = new LinkedList<>();
    private final ScheduledExecutorService executor;
    private final Duration initialDelay;
    private final Duration period;

    /*
     * Runtime options.
     */
    private final List<TargetRuntime> runtimes = Collections.synchronizedList(new LinkedList<>());

    private FileChangeWatcher(Builder builder) {
        this.executor = builder.executor;
        this.watchServiceModifiers.addAll(builder.watchServiceModifiers);
        this.initialDelay = builder.initialDelay;
        this.period = builder.period;
    }

    /**
     * Fluent API builder for {@link FileChangeWatcher}.
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Create a new file watcher with default configuration.
     *
     * @return a new file watcher
     */
    public static FileChangeWatcher create() {
        return builder().build();
    }

    @Override
    public synchronized void start(Path target, Consumer<ChangeEvent<Path>> listener) {
        if (executor.isShutdown()) {
            throw new IllegalStateException("Cannot start a watcher for path " + target + ", as the executor service is "
                                                + "shutdown");
        }

        Monitor monitor = new Monitor(
            listener,
            target,
            watchServiceModifiers);

        ScheduledFuture<?> future = executor.scheduleWithFixedDelay(monitor::run,
                                                                    initialDelay.toMillis(),
                                                                    period.toMillis(),
                                                                    TimeUnit.MILLISECONDS);

        this.runtimes.add(new TargetRuntime(monitor, future));
    }

    @Override
    public synchronized void stop() {
        runtimes.forEach(TargetRuntime::stop);
    }

    /**
     * Add modifiers to be used when registering the {@link java.nio.file.WatchService}.
     * See {@link java.nio.file.Path#register(java.nio.file.WatchService, java.nio.file.WatchEvent.Kind[],
     * java.nio.file.WatchEvent.Modifier...) Path.register}.
     *
     * @param modifiers the modifiers to add
     */
    public void initWatchServiceModifiers(WatchEvent.Modifier... modifiers) {
        watchServiceModifiers.addAll(Arrays.asList(modifiers));
    }

    private static final class TargetRuntime {
        private final Monitor monitor;
        private final ScheduledFuture<?> future;

        private TargetRuntime(Monitor monitor, ScheduledFuture<?> future) {
            this.monitor = monitor;
            this.future = future;
        }

        public void stop() {
            monitor.stop();
            future.cancel(true);
        }
    }

    private static final class Monitor implements Runnable {
        private final WatchService watchService;
        private final Consumer<ChangeEvent<Path>> listener;
        private final Path target;
        private final List<WatchEvent.Modifier> watchServiceModifiers;
        private final boolean watchingFile;
        private final Path watchedDir;

        /*
         * Runtime handling
         */
        // we have failed - retry registration on next trigger
        private volatile boolean failed = true;
        // maybe we were stopped, do not do anything (the scheduled future will be cancelled shortly)
        private volatile boolean shouldStop = false;
        // last file information
        private volatile boolean fileExists;

        private WatchKey watchKey;

        private Monitor(Consumer<ChangeEvent<Path>> listener,
                        Path target,
                        List<WatchEvent.Modifier> watchServiceModifiers) {
            try {
                this.watchService = FileSystems.getDefault().newWatchService();
            } catch (IOException e) {
                throw new IllegalStateException("Cannot obtain WatchService.", e);
            }
            this.listener = listener;
            this.target = target;
            this.watchServiceModifiers = watchServiceModifiers;
            this.fileExists = Files.exists(target);
            this.watchingFile = !Files.isDirectory(target);
            this.watchedDir = watchingFile ? target.toAbsolutePath().getParent() : target;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void run() {
            try {
                doRun();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Change watcher failed on directory: "
                               + watchedDir + ", target: " + target.toAbsolutePath(),
                           e);
            }
        }

        private void doRun() {
            if (shouldStop) {
                return;
            }

            if (failed) {
                register();
            }

            if (failed) {
                return;
            }

            // if we used `take`, we would block the thread forever. This way we can use the same thread to handle
            // multiple targets
            WatchKey key = watchService.poll();
            if (null == key) {
                return;
            }

            List<WatchEvent<?>> watchEvents = key.pollEvents();
            if (watchEvents.isEmpty()) {
                // something happened, cannot get details
                key.cancel();
                listener.accept(ChangeEvent.create(target, ChangeEventType.CHANGED));
                failed = true;
                return;
            }

            // we actually have some changes
            for (WatchEvent<?> watchEvent : watchEvents) {
                WatchEvent<Path> event = (WatchEvent<Path>) watchEvent;
                Path eventPath = event.context();

                // as we watch on whole directory
                // make sure this is the watched file (if only interested in a single file)
                if (watchingFile && !target.endsWith(eventPath)) {
                    continue;
                }

                eventPath = watchedDir.resolve(eventPath);
                WatchEvent.Kind<Path> kind = event.kind();
                if (kind.equals(OVERFLOW)) {
                    LOGGER.finest("Overflow event on path: " + eventPath);
                    continue;
                }

                if (kind.equals(ENTRY_CREATE)) {
                    LOGGER.finest("Entry created. Path: " + eventPath);
                    listener.accept(ChangeEvent.create(eventPath, ChangeEventType.CREATED));
                } else if (kind == ENTRY_DELETE) {
                    LOGGER.finest("Entry deleted. Path: " + eventPath);
                    listener.accept(ChangeEvent.create(eventPath, ChangeEventType.DELETED));
                } else if (kind == ENTRY_MODIFY) {
                    LOGGER.finest("Entry changed. Path: " + eventPath);
                    listener.accept(ChangeEvent.create(eventPath, ChangeEventType.CHANGED));
                }
            }

            if (!key.reset()) {
                LOGGER.log(Level.FINE, () -> "Directory of '" + target + "' is no more valid to be watched.");
                failed = true;
            }
        }

        private void fire(Path target, ChangeEventType eventType) {
            listener.accept(ChangeEvent.create(target, eventType));
        }

        private synchronized void register() {
            if (shouldStop) {
                failed = true;
                return;
            }

            boolean oldFileExists = fileExists;

            try {
                Path cleanTarget = target(this.target);
                Path watchedDirectory = Files.isDirectory(cleanTarget) ? cleanTarget : parentDir(cleanTarget);

                WatchKey oldWatchKey = watchKey;
                watchKey = watchedDirectory.register(watchService,
                                                     new WatchEvent.Kind[] {ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE},
                                                     watchServiceModifiers.toArray(new WatchEvent.Modifier[0]));

                failed = false;
                if (null != oldWatchKey) {
                    oldWatchKey.cancel();
                }
            } catch (IOException e) {
                LOGGER.log(Level.FINEST, "Failed to register watch service", e);
                this.failed = true;
            }

            // in either case, let's see if our target has changed
            this.fileExists = Files.exists(target);

            if (fileExists != oldFileExists) {
                if (fileExists) {
                    fire(this.target, ChangeEventType.CREATED);
                } else {
                    fire(this.target, ChangeEventType.DELETED);
                }
            }
        }

        private synchronized void stop() {
            this.shouldStop = true;
            if (null != watchKey) {
                watchKey.cancel();
            }
            try {
                watchService.close();
            } catch (IOException e) {
                LOGGER.log(Level.FINE, "Failed to close watch service", e);
            }
        }

        private Path target(Path path) throws IOException {
            Path target = path;
            while (Files.isSymbolicLink(target)) {
                target = target.toRealPath();
            }
            return target.toAbsolutePath();
        }

        private Path parentDir(Path path) {
            Path parent = path.getParent();
            if (parent == null) {
                throw new IllegalStateException(
                    String.format("Cannot find parent directory for '%s' to register watch service.", path));
            }
            return parent;
        }
    }

    /**
     * Fluent API builder for {@link jakarta.config.impl.FileChangeWatcher}.
     */
    public static final class Builder implements jakarta.common.Builder<Builder, FileChangeWatcher> {
        private final List<WatchEvent.Modifier> watchServiceModifiers = new LinkedList<>();
        private Duration initialDelay = Duration.ofSeconds(1);
        private Duration period = Duration.ofSeconds(1);
        private ScheduledExecutorService executor;

        private Builder() {
        }

        @Override
        public FileChangeWatcher build() {
            return new FileChangeWatcher(this);
        }

        /**
         * Executor to use for this watcher.
         * The task is scheduled for regular execution and is only blocking a thread for the time needed
         * to process changed files.
         *
         * @param executor executor service to use
         * @return updated builder instance
         */
        public Builder executor(ScheduledExecutorService executor) {
            this.executor = executor;
            return this;
        }

        /**
         * Duration to wait before first scheduled check.
         *
         * @param initialDelay initial delay
         * @return updated builder
         */
        public Builder initialDelay(Duration initialDelay) {
            this.initialDelay = initialDelay;
            return this;
        }

        /**
         * Configure schedule of the file watcher.
         *
         * @param period duration between checks
         * @return updated builder instance
         */
        public Builder period(Duration period) {
            this.period = period;
            return this;
        }

        /**
         * Add a modifier of the watch service.
         * Currently only implementation specific modifier are available, such as
         * {@code com.sun.nio.file.SensitivityWatchEventModifier}.
         *
         * @param modifier modifier to use
         * @return updated builder instance
         */
        public Builder addWatchServiceModifier(WatchEvent.Modifier modifier) {
            this.watchServiceModifiers.add(modifier);
            return this;
        }

        /**
         * Set modifiers to use for the watch service.
         * Currently only implementation specific modifier are available, such as
         * {@code com.sun.nio.file.SensitivityWatchEventModifier}.
         *
         * @param modifiers modifiers to use (replacing current configuration)
         * @return updated builder instance
         */
        public Builder watchServiceModifiers(List<WatchEvent.Modifier> modifiers) {
            this.watchServiceModifiers.clear();
            this.watchServiceModifiers.addAll(modifiers);

            return this;
        }
    }
}

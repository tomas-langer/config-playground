package jakarta.config.impl;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.config.spi.ConfigSource;

class ConfigSourceInfo {
    private AtomicReference<ConfigSource> sourceRef = new AtomicReference<>();
    private AtomicReference<Integer> priorityRef = new AtomicReference<>();

    public Optional<ConfigSource> configSource() {
        return Optional.ofNullable(sourceRef.get());
    }

    public Optional<Integer> sourcePriority() {
        return Optional.ofNullable(priorityRef.get());
    }

    public void configSource(ConfigSource source) {
        sourceRef.set(source);
    }

    public void sourcePriority(int priority) {
        priorityRef.set(priority);
    }
}

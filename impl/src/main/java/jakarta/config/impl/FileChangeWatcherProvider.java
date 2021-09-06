package jakarta.config.impl;

import java.nio.file.Path;
import java.util.concurrent.ScheduledExecutorService;

import jakarta.config.spi.ChangeWatcher;
import jakarta.config.spi.ChangeWatcherProvider;

public class FileChangeWatcherProvider implements ChangeWatcherProvider {
    @Override
    public boolean support(Class<?> type) {
        return type.equals(Path.class);
    }

    @Override
    public boolean reusable() {
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> ChangeWatcher<T> create(Class<T> type, ScheduledExecutorService executor) {
        return (ChangeWatcher<T>) FileChangeWatcher.builder()
            .executor(executor)
            .build();
    }
}

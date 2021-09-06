package jakarta.config.spi;

import java.util.concurrent.ScheduledExecutorService;

/**
 * Service provider.
 */
public interface ChangeWatcherProvider {
    /**
     * Return {@code true} if this provider can create {@link jakarta.config.spi.ChangeWatcher}
     * of the requested type.
     *
     * @param type type to check
     * @return {@code true} if the type is supported by this provider
     */
    boolean support(Class<?> type);

    /**
     * Return {@code true} in case the same instance can be used to watch multiple targets of the same type.
     *
     * @return whether an instance of {@link jakarta.config.spi.ChangeWatcher} can be reused, defaults to {@code false}
     */
    default boolean reusable() {
        return false;
    }
    /**
     * Create a change watcher instance.
     *
     * @param type type of the target
     * @param executor scheduled executor service
     * @param <T> type of the change watcher target
     *
     * @return a new change watcher
     */
    <T> ChangeWatcher<T> create(Class<T> type, ScheduledExecutorService executor);
}

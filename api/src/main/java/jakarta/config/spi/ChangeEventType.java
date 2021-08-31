package jakarta.config.spi;

/**
 * Type of changes that can happen in a {@link jakarta.config.spi.PollingStrategy.Polled}
 * components and sent to consumer
 * {@link jakarta.config.spi.ChangeWatcher#start(Object, java.util.function.Consumer)}.
 */
public enum ChangeEventType {
    /**
     * Nothing is changed.
     */
    UNCHANGED,
    /**
     * The content is modified.
     */
    CHANGED,
    /**
     * The content is not present.
     */
    DELETED,
    /**
     * The content is now present (was deleted).
     */
    CREATED
}

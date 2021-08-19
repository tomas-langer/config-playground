package jakarta.config.full.spi;

public interface WatchableConfigSource<T> {
    /**
     * Target type as supported by this source.
     *
     * @return class of the target, by default used for {@link #target()}
     */
    @SuppressWarnings("unchecked")
    default Class<T> targetType() {
        return (Class<T>) target().getClass();
    }

    /**
     * The target of this source.
     *
     * @return target this source is configured with, never {@code null}
     */
    T target();

}

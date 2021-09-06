package jakarta.common;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Common ancestor for all builders.
 *
 * @param <B> type of the builder extending this builder
 * @param <T> built type
 */
public interface Builder<B extends Builder<B, T>, T> extends Supplier<T> {
    /**
     * Build an instance from this builder.
     * This method can be invoked multiple times and should produce independent instances.
     *
     * @return a new instance of the built type created from this builder
     */
    T build();

    @Override
    default T get() {
        return build();
    }

    /**
     * Helper method to fluently update this builder.
     *
     * @param builderConsumer consumer of this builder
     * @return updated builder instance
     */
    @SuppressWarnings("unchecked")
    default B update(Consumer<B> builderConsumer) {
        builderConsumer.accept((B) this);
        return (B) this;
    }

}

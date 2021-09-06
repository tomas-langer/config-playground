package jakarta.common;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * An object with a priority.
 * @param <T> type of the object
 */
public class PrioritizedObject<T> implements Prioritized, Supplier<T> {
    private final T object;
    private final int priority;

    private PrioritizedObject(T object, int priority) {
        this.object = object;
        this.priority = priority;
    }

    /**
     * Create a new builder to customize instantiation.
     *
     * @param object object to use
     * @param <T> type of the object
     * @return a new builder
     */
    public static <T> Builder<T> builder(T object) {
        Objects.requireNonNull(object, "Prioritized object must not be null");
        return new Builder<>(object);
    }

    /**
     * Create a new prioritized object finding priority from the object instance.
     * Priority is determined in the following order:
     * <ul>
     *     <li>Priority of the instance if it implements {@link jakarta.common.Prioritized}</li>
     *     <li>Priority of the {@link jakarta.annotation.Priority} annotation</li>
     *     <li>Default priority - {@link Prioritized#DEFAULT_PRIORITY}</li>
     * </ul>
     *
     * @param instance instance to use
     * @param <T> type of the instance
     * @return a new prioritized object
     */
    public static <T> PrioritizedObject<T> create(T instance) {
        return builder(instance).build();
    }

    /**
     * Create a new prioritized object with explicit priority.
     *
     * @param instance instance to use
     * @param priority explicit priority of this instance
     * @param <T> type of the instance
     * @return a new prioritized object
     */
    public static <T> PrioritizedObject<T> create(T instance, int priority) {
        return builder(instance).priority(priority).build();
    }

    @Override
    public int priority() {
        return priority;
    }

    @Override
    public T get() {
        return object;
    }

    /**
     * Fluent API builder for {@link jakarta.common.PrioritizedObject}.
     * @param <T> type of the object
     */
    public static class Builder<T> implements jakarta.common.Builder<Builder<T>, PrioritizedObject<T>> {
        private final T object;
        private Integer priority;
        private int defaultPriority = DEFAULT_PRIORITY;

        public Builder(T object) {
            this.object = object;
        }

        @Override
        public PrioritizedObject<T> build() {
            if (priority == null) {
                priority = Priorities.find(object, defaultPriority);
            }

            return new PrioritizedObject<>(object, priority);
        }

        /**
         * Explicit priority.
         *
         * @param priority priority to use
         * @return updated builder
         */
        public Builder<T> priority(Integer priority) {
            this.priority = priority;
            return this;
        }

        /**
         * Default priority to use if no explicit priority is configured and a priority cannot
         * be determined from the object instance.
         *
         * @param defaultPriority default priority
         * @return updated builder
         */
        public Builder<T> defaultPriority(int defaultPriority) {
            this.defaultPriority = defaultPriority;
            return this;
        }
    }
}

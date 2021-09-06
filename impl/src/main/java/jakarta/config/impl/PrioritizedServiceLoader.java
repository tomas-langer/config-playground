package jakarta.config.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import jakarta.common.Prioritized;
import jakarta.common.PrioritizedObject;

/**
 * Helidon specific support for Java Service Loaders.
 * <p>
 * This service loader:
 * <ul>
 * <li>Can have additional implementations added</li>
 * <li>Uses priorities defined either by {@link jakarta.common.Prioritized}
 * or by {@link jakarta.annotation.Priority}</li>
 * <li>Can have exclusions defined by an exact implementation class name, either
 * in {@link Builder#addExcludedClass(Class)} or {@link Builder#addExcludedClassName(String)} or
 * by a system property {@value #SYSTEM_PROPERTY_EXCLUDE} that defines
 * a comma separated list of fully qualified class names to be excluded.
 * Note that if a class implements more than one service, it would be excluded from all.</li>
 * </ul>
 * <p>
 * <i>Note on priority handling</i>
 * <p>
 * Service priority is defined by:
 * <ul>
 * <li>Value provided in {@link Builder#addService(Object, int)} (if used)</li>
 * <li>then by {@link jakarta.common.Prioritized#priority()} if service implements it</li>
 * <li>then by {@link jakarta.annotation.Priority} annotation if present</li>
 * <li>otherwise a default priority {@value jakarta.common.Prioritized#DEFAULT_PRIORITY} from {@link jakarta.common.Prioritized#DEFAULT_PRIORITY} is used</li>
 * </ul>
 * Example:
 * <pre>
 * {@literal @}Priority(4500)
 * public class MyServiceImpl implements Service, Prioritized {
 *     public int priority() {
 *         return 6200;
 *     }
 * }
 * </pre>
 * Such a service would have a priority of {@code 6200} as that is more significant than the annotation.
 * <p>
 * A service with lower priority number is returned before a service with a higher priority number.
 * Services with the same priority have order defined by the order they are in the configured services
 * and then as they are loaded from the {@link java.util.ServiceLoader}.
 * Negative priorities are not allowed.
 * A service with priority {@code 1} will be returned before a service with priority {@code 2}.
 *
 * @param <T> Type of the service to be loaded
 * @see java.util.ServiceLoader
 * @see #builder(Class)
 */
public final class PrioritizedServiceLoader<T> implements Iterable<T> {
    /**
     * System property used to exclude some implementation from the list of services that are configured for Java Service
     * loader or services that are registered using {@link Builder}.
     */
    public static final String SYSTEM_PROPERTY_EXCLUDE = "jakarta.serviceloader.exclude";

    private static final Logger LOGGER = Logger.getLogger(PrioritizedServiceLoader.class.getName());

    private final List<T> services;

    private PrioritizedServiceLoader(List<T> services) {
        this.services = new LinkedList<>(services);
    }

    /**
     * Create a builder for customizable service loader.
     *
     * @param service the interface/class of the service
     * @param <T>     type of the service
     * @return a new fluent API builder
     */
    // TODO if we wanted this to be a shared class, we would need to get the ServiceLoader as parameter
    // as otherwise the common module would not have right to read the services
    public static <T> Builder<T> builder(Class<T> service) {
        return new Builder<>(service);
    }

    /**
     * Create a prioritized service loader from a Java Service loader.
     *
     * @param service the interface/class of the service
     * @param <T>     type of the service
     * @return service loader with exclusions defined by system properties and no custom services
     */
    public static <T> PrioritizedServiceLoader<T> create(Class<T> service) {
        return builder(service).build();
    }

    @Override
    public Iterator<T> iterator() {
        return List.copyOf(services)
            .iterator();
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        this.services.forEach(action);
    }

    /**
     * Provides a list of service implementations in prioritized order.
     *
     * @return list of service implementations
     */
    public List<T> asList() {
        return List.copyOf(this.services);
    }

    /**
     * Fluent api builder for {@link PrioritizedServiceLoader}.
     *
     * @param <T> type of the service to be loaded
     */
    public static final class Builder<T> implements jakarta.common.Builder<Builder<T>, PrioritizedServiceLoader<T>> {
        private final List<PrioritizedObject<T>> customServices = new LinkedList<>();
        private final Set<String> excludedServiceClasses = new HashSet<>();
        private final Class<T> service;

        private boolean useSysPropExclude = true;
        private boolean useSystemServiceLoader = true;
        private boolean replaceImplementations = true;
        private int defaultPriority = Prioritized.DEFAULT_PRIORITY;

        private Builder(Class<T> service) {
            this.service = service;
        }

        @Override
        public PrioritizedServiceLoader<T> build() {
            // first merge the lists together
            List<PrioritizedObject<T>> services = new LinkedList<>(customServices);
            if (useSystemServiceLoader) {
                Set<String> uniqueImplementations = new HashSet<>();

                if (replaceImplementations) {
                    customServices.stream()
                        .map(it -> it.get().getClass().getName())
                        .forEach(uniqueImplementations::add);
                }

                ServiceLoader.load(service).forEach(service -> {
                    if (!replaceImplementations || !uniqueImplementations.contains(service.getClass().getName())) {
                        services.add(PrioritizedObject.builder(service)
                                         .defaultPriority(defaultPriority)
                                         .build());
                    }
                });
            }

            if (useSysPropExclude) {
                addSystemExcludes();
            }
            List<PrioritizedObject<T>> withoutExclusions = services.stream()
                .filter(this::notExcluded)
                .collect(Collectors.toList());

            // order by priority
            return new PrioritizedServiceLoader<>(orderByPriority(withoutExclusions));
        }

        /**
         * When configured to use system excludes, system property {@value #SYSTEM_PROPERTY_EXCLUDE} is used to get the
         * comma separated list of service implementations to exclude them from the loaded list.
         * <p>
         * This defaults to {@code true}.
         *
         * @param useSysPropExclude whether to use a system property to exclude service implementations
         * @return updated builder instance
         */
        public Builder<T> useSystemExcludes(boolean useSysPropExclude) {
            this.useSysPropExclude = useSysPropExclude;
            return this;
        }

        /**
         * When configured to use Java Service loader, then the result is a combination of all service implementations
         * loaded from the {@link java.util.ServiceLoader} and those added by {@link #addService(Object)}
         * or {@link #addService(Object, int)}.
         * When set to {@code false} the Java Service loader is not used.
         * <p>
         * This defaults to {@code true}.
         *
         * @param useServiceLoader whether to use the Java Service loader
         * @return updated builder instance
         */
        public Builder<T> useSystemServiceLoader(boolean useServiceLoader) {
            this.useSystemServiceLoader = useServiceLoader;
            return this;
        }

        /**
         * When configured to replace implementations, then a service implementation configured through
         * {@link #addService(Object)}
         * will replace the same implementation loaded from the Java Service loader (compared by fully qualified class name).
         * <p>
         * This defaults to {@code true}.
         *
         * @param replace whether to replace service instances loaded by java service loader with the ones provided
         *                through builder methods
         * @return updated builder instance
         */
        public Builder<T> replaceImplementations(boolean replace) {
            this.replaceImplementations = replace;
            return this;
        }

        /**
         * Add a custom service implementation to the list of services.
         *
         * @param service a new service instance
         * @return updated builder instance
         */
        public Builder<T> addService(T service) {
            this.customServices.add(PrioritizedObject.builder(service)
                                        .defaultPriority(defaultPriority)
                                        .build());
            return this;
        }

        /**
         * Add a custom service implementation to the list of services with a custom priority.
         *
         * @param service  a new service instance
         * @param priority priority to use when ordering service instances
         * @return updated builder instance
         */
        public Builder<T> addService(T service, int priority) {
            this.customServices.add(PrioritizedObject.create(service, priority));
            return this;
        }

        /**
         * Add an excluded implementation class - if such a service implementation is configured (either through
         * Java Service loader or through {@link #addService(Object)}), it would be ignored.
         *
         * @param excluded excluded implementation class
         * @return updated builder instance
         */
        public Builder<T> addExcludedClass(Class<? extends T> excluded) {
            excludedServiceClasses.add(excluded.getName());
            return this;
        }

        /**
         * Add an excluded implementation class - if such a service implementation is configured (either through
         * Java Service loader or through {@link #addService(Object)}), it would be ignored.
         *
         * @param excludeName excluded implementation class name
         * @return updated builder instance
         */
        public Builder<T> addExcludedClassName(String excludeName) {
            excludedServiceClasses.add(excludeName);
            return this;
        }

        /**
         * Configure default priority for services that do not have any.
         *
         * @param defaultPriority default priority to use, defaults to {@link Prioritized#DEFAULT_PRIORITY}
         * @return updated builder instance
         */
        public Builder<T> defaultPriority(int defaultPriority) {
            this.defaultPriority = defaultPriority;
            return this;
        }

        private boolean notExcluded(PrioritizedObject<T> service) {
            String className = service.get().getClass().getName();
            if (excludedServiceClasses.contains(className)) {
                LOGGER.finest(() -> "Excluding service implementation " + className);
                return false;
            }
            return true;
        }

        private List<T> orderByPriority(List<PrioritizedObject<T>> services) {
            Collections.sort(services);

            List<T> result = services.stream()
                .map(Supplier::get)
                .collect(Collectors.toList());

            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.finest("Final order of enabled service implementations for service: " + service);
                result.stream()
                    .map(Object::getClass)
                    .map(Class::getName)
                    .forEach(LOGGER::finest);
            }

            return result;
        }

        private void addSystemExcludes() {
            String excludes = System.getProperty(SYSTEM_PROPERTY_EXCLUDE);
            if (null == excludes) {
                return;
            }

            for (String exclude : excludes.split(",")) {
                LOGGER.finest(() -> "Adding exclude from system properties: " + exclude);
                addExcludedClassName(exclude);
            }
        }
    }
}

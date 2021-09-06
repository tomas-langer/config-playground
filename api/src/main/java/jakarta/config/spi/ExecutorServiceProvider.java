package jakarta.config.spi;

import java.util.concurrent.ScheduledExecutorService;

/**
 * Service provider for scheduled executor services.
 */
public interface ExecutorServiceProvider {
    /**
     * Provide an executor service used to schedule any scheduled tasks
     * of Jakarta Config.
     *
     * @return scheduled executor service to use
     */
    ScheduledExecutorService service();
}

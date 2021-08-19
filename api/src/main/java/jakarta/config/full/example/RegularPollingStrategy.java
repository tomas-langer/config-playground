package jakarta.config.full.example;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.config.full.spi.PollingStrategy;

public class RegularPollingStrategy implements PollingStrategy {
    private static final Logger LOGGER = Logger.getLogger(RegularPollingStrategy.class.getName());

    private final Duration delay;
    private ScheduledExecutorService executor;
    private ScheduledFuture<?> scheduledFuture;
    private volatile Polled polled;

    public RegularPollingStrategy(Duration delay) {
        this.delay = delay;
    }

    @Override
    public void start(Polled polled) {
        if (executor == null || executor.isShutdown()) {
            executor = Executors.newSingleThreadScheduledExecutor();
        }

        this.polled = polled;
        scheduleNext();
    }

    @Override
    public void stop() {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
        }
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    private void scheduleNext() {
        try {
            scheduledFuture = executor.schedule(this::fireEvent,
                                                delay.toMillis(),
                                                TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException e) {
            if (executor.isShutdown()) {
                // intentional shutdown of an executor service
                LOGGER.log(Level.FINEST, "Executor service is shut down, polling is terminated for " + this, e);
            } else {
                // exceptional condition
                LOGGER.log(Level.SEVERE, "Failed to schedule next polling for " + this + ", polling will stop", e);
            }
        }
    }

    private synchronized void fireEvent() {
        polled.poll(Instant.now());
        scheduleNext();
    }
}

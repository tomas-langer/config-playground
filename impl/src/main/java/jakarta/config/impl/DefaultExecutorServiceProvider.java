package jakarta.config.impl;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import jakarta.common.LazyValue;
import jakarta.common.Prioritized;
import jakarta.config.spi.ExecutorServiceProvider;

public class DefaultExecutorServiceProvider implements ExecutorServiceProvider, Prioritized {
    // using lazy value, so the service is only instantiated when requested.
    private static final LazyValue<ScheduledExecutorService> SERVICE_HOLDER = LazyValue.create(() -> {
        return Executors.newSingleThreadScheduledExecutor(new ConfigThreadFactory("config-changes"));
    });

    @Override
    public ScheduledExecutorService service() {
        return SERVICE_HOLDER.get();
    }

    @Override
    public int priority() {
        // this is the lowest priority, as it creates a new instance
        return DEFAULT_PRIORITY + 1000;
    }
}

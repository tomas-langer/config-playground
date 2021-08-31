package jakarta.config;

public class ReservedKeys {
    // maybe all properties prefixed with "jakarta_config_"

    /**
     * Priority of config source.
     */
    public static final String CONFIG_PRIORITY = "config_priority";
    /**
     * If we significantly change the way source is loaded, we may have a possibility to
     * load it in the old approach.
     */
    public static final String CONFIG_VERSION = "config_version";

    /**
     * Whether polling is enabled if the config source implements {@link jakarta.config.spi.PollableConfigSource},
     *  configuration key is {@value}.
     *
     * Defaults to {@code true}.
     */
    public static final String CONFIG_POLLING_ENABLED = "config_polling_enabled";

    /**
     * Duration of polling if the config source implements {@link jakarta.config.spi.PollableConfigSource},
     *  configuration key is {@value}.
     *
     * Defaults to {@code PT10S} (10 seconds) if enabled.
     */
    public static final String CONFIG_POLLING_DURATION = "config_polling_duration";
}

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
     * Whether any type of change support is enabled on this source.
     * If a source implements any interface with change support, this defaults to true.
     * Configuration key is {@value}.
     */
    public static final String CHANGE_SUPPORT_ENABLED = "config_changes_enabled";
    /**
     * If a config source implements both {@link jakarta.config.spi.WatchableConfigSource}
     * and {@link jakarta.config.spi.PollableConfigSource},
     * this property can be used to disable change watching even if supported by config. In such a case
     * polling would be used. Configuration key is {@value}.
     *
     * @see #CONFIG_POLLING_ENABLED
     * @see #CHANGE_SUPPORT_ENABLED
     */
    public static final String CHANGE_WATCHER_ENABLED = "config_change_watcher_enabled";
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

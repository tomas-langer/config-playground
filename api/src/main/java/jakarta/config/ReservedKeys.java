package jakarta.config;

public class ReservedKeys {
    /**
     * Priority of config source.
     */
    public static final String CONFIG_PRIORITY = "config_priority";
    /**
     * If we significantly change the way source is loaded, we may have a possibility to
     * load it in the old approach.
     */
    public static final String CONFIG_VERSION = "config_version";
    // maybe all properties prefixed with "jakarta_config_"
}

import jakarta.config.spi.ConfigProviderResolver;

/**
 * Jakarta Config API.
 */
module jakarta.config {
    requires java.logging;

    exports jakarta.config;
    exports jakarta.config.spi;

    uses ConfigProviderResolver;
}

/**
 * Jakarta Config API.
 */
module jakarta.config {
    requires java.logging;
    exports jakarta.config;
    exports jakarta.config.flat.spi;
    exports jakarta.config.flat;
    exports jakarta.config.tree.spi;
    exports jakarta.config.tree;

    uses jakarta.config.flat.spi.ConfigProviderResolver;
}

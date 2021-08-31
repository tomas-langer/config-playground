module jakarta.config.impl {

    requires java.logging;
    requires jakarta.annotation;
    requires jakarta.config;
    requires org.yaml.snakeyaml;

    exports jakarta.config.impl;

    uses jakarta.config.spi.ConfigSource;
    uses jakarta.config.spi.ConfigSourceProvider;
    uses jakarta.config.spi.Converter;
    uses jakarta.config.spi.ConfigParser;

    provides jakarta.config.spi.ConfigProviderResolver with jakarta.config.impl.ConfigProviderResolverImpl;
    provides jakarta.config.spi.ConfigParser with jakarta.config.impl.PropertiesParser,
        jakarta.config.impl.YamlParser;
}

import jakarta.config.impl.ConfigProviderResolverImpl;
import jakarta.config.impl.DefaultExecutorServiceProvider;
import jakarta.config.impl.FileChangeWatcherProvider;
import jakarta.config.impl.PropertiesParser;
import jakarta.config.impl.YamlParser;

module jakarta.config.impl {

    requires java.logging;
    requires jakarta.annotation;
    requires jakarta.config;
    requires org.yaml.snakeyaml;

    exports jakarta.config.impl;
    exports jakarta.common;

    uses jakarta.config.spi.ConfigSource;
    uses jakarta.config.spi.ConfigSourceProvider;
    uses jakarta.config.spi.Converter;
    uses jakarta.config.spi.ConfigParser;
    uses jakarta.config.spi.ExecutorServiceProvider;
    uses jakarta.config.spi.ChangeWatcherProvider;

    provides jakarta.config.spi.ExecutorServiceProvider with DefaultExecutorServiceProvider;
    provides jakarta.config.spi.ConfigProviderResolver with ConfigProviderResolverImpl;
    provides jakarta.config.spi.ConfigParser with PropertiesParser, YamlParser;
    provides jakarta.config.spi.ChangeWatcherProvider with FileChangeWatcherProvider;
}

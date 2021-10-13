package jakarta.config.tck.programmatic;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.config.Config;
import jakarta.config.ConfigValue;
import jakarta.config.ReservedKeys;
import jakarta.config.spi.ConfigProviderResolver;
import jakarta.config.spi.ConfigSource;
import jakarta.config.spi.StringConverter;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test config resolver and its methods.
 */
public class ConfigResolverTest {
    private static ConfigProviderResolver configProviderResolver;

    @BeforeClass
    static void init() {
        configProviderResolver = ConfigProviderResolver.instance();
    }

    @Test
    void testBuilder() {
        Config config = configProviderResolver.getBuilder()
            .withSources(new TestConfigSource())
            .withConverters(new TestConverter())
            .withConverter(TestPojo.class, 100, (StringConverter<TestPojo>) TestPojo::new)
            .build();

        Config serverNode = config.get("server");
        assertThat(serverNode.asString(), is(Optional.of("value")));

        Config hostNode = serverNode.get("host");
        assertThat(hostNode.asString(), is(Optional.of("localhost")));
        hostNode = config.get("server.host");
        assertThat(hostNode.asString(), is(Optional.of("localhost")));
        assertThat(hostNode.name(), is("host"));
        assertThat(hostNode.key(), is("server.host"));

        Config portNode = serverNode.get("port");
        assertThat(portNode.asString(), is(Optional.of("7001")));
        assertThat(portNode.asInt(), is(Optional.of(7001)));

        Optional<ConfigValue> configValueOpt = serverNode.get("name").configValue();
        assertThat(configValueOpt, not(Optional.empty()));

        ConfigValue configValue = configValueOpt.get();

        assertThat(configValue.getKey(), is("server.name"));
        assertThat(configValue.getRawValue(), is("${name}"));
        assertThat(configValue.getValue(), is("TckTest"));
        assertThat(configValue.getSourceName(), is("TCK"));
        assertThat(configValue.getSourcePriority(), is(542));
    }

    private static class TestConfigSource implements ConfigSource {
        private final Map<String, String> values = Map.of(ReservedKeys.SOURCE_PRIORITY, "542",
                                                          "name", "TckTest",
                                                          "server", "value",
                                                          "server.host", "localhost",
                                                          "server.port", "7001",
                                                          "server.name", "${name}");

        @Override
        public String getName() {
            return "TCK";
        }

        @Override
        public String getValue(String key) {
            return values.get(key);
        }

        @Override
        public Set<String> getKeys() {
            return values.keySet();
        }
    }

    private static class TestConverter implements StringConverter<TestPojoConverted> {
        @Override
        public TestPojoConverted convert(String value) throws IllegalArgumentException, NullPointerException {
            return new TestPojoConverted(value);
        }
    }

    private static class TestPojoConverted {
        private final String value;

        private TestPojoConverted(String value) {
            this.value = value;
        }
    }

    private static class TestPojo {
        private final String value;

        private TestPojo(String value) {
            this.value = value;
        }
    }


}

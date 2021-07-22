package jakarta.config.example;

import java.net.URI;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;

import jakarta.config.Config;
import jakarta.config.Configured;
import jakarta.config.cdi.ConfigValue;
import jakarta.config.cdi.ConfigValues;

@ApplicationScoped
public class CdiExampleBean {
    private final Person person;
    private final String name;
    private final Config personConfig;
    private final Config config;

    @Inject
    public CdiExampleBean(@ConfigValues Person person,
                          @ConfigValue("app.person.name") String name,
                          @ConfigValue("app.person") Config personConfig,
                          Config config) {
        this.person = person;
        this.name = name;
        this.personConfig = personConfig;
        this.config = config;
    }

    public void configure(@QueryParam("count") int count) {

    }

    public Person getPerson() {
        return person;
    }

    public String getName() {
        return name;
    }

    public Config getPersonConfig() {
        return personConfig;
    }

    public Config getConfig() {
        return config;
    }

    // how should an implementation discover this class?
    // it is not a bean, so we would need to do a classpath scan, otherwise
    // it would never be available for programmatic injection!!!
    @ConfigValues("app.person")
    public interface Person {
        @Configured(defaultValue = "Jane Doe")
        String name();
        @Configured("year-of-birth")
        int birthYear();
        URI homepage();
    }
}

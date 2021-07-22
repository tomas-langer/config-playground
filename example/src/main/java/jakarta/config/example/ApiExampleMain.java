package jakarta.config.example;

import java.net.URI;

import jakarta.config.Config;
import jakarta.config.ConfigProvider;
import jakarta.config.Configured;

public class ApiExampleMain {
    public static void main(String[] args) {
        defaults();
    }

    private static void defaults() {
        // get default configuration
        Config config = ConfigProvider.getConfig();

        String name = config.getValue("app.person.name", String.class);
        Person configured = config.getValue("app.person", Person.class);

        System.out.println("Name: " + name);
        System.out.println("Configured: " + configured);
    }

    public interface Person {
        @Configured(defaultValue = "Jane Doe")
        String name();
        @Configured("year-of-birth")
        int birthYear();
        URI homepage();
    }
}

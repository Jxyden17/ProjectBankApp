package nl.donniebankoebarkie.api.config;

import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

@Component
public class ApplicationEnvironment {
    public static final String DEVELOPMENT_PROFILE = "dev";
    public static final String PRODUCTION_PROFILE = "prod";

    private final Environment environment;

    public ApplicationEnvironment(Environment environment) {
        this.environment = environment;
    }

    public boolean isDevelopment() {
        return environment.acceptsProfiles(Profiles.of(DEVELOPMENT_PROFILE));
    }

    public boolean isProduction() {
        return environment.acceptsProfiles(Profiles.of(PRODUCTION_PROFILE));
    }
}

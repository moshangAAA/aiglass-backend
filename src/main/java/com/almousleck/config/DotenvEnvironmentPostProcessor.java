package com.almousleck.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Dotenv dotenv = Dotenv.configure()
                .directory(".")
                .ignoreIfMissing()
                .load();

        Map<String, Object> dotEnvProps = new HashMap<>();
        dotenv.entries().forEach(entry -> dotEnvProps.put(entry.getKey(), entry.getValue()));

        // Host redirection logic for localhost
        String url = (String) dotEnvProps.get("SPRING_DATASOURCE_URL");
        if (url == null) {
            url = environment.getProperty("SPRING_DATASOURCE_URL");
        }
        
        if (url != null && url.contains("jdbc:mysql://mysql:")) {
            String localUrl = url.replace("jdbc:mysql://mysql:", "jdbc:mysql://localhost:");
            if (!localUrl.contains("?")) {
                localUrl += "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
            }
            dotEnvProps.put("SPRING_DATASOURCE_URL", localUrl);
        }

        if (!dotEnvProps.isEmpty()) {
            environment.getPropertySources().addFirst(new MapPropertySource("dotenvProperties", dotEnvProps));
        }
    }
}

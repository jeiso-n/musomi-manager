package com.musomi.manager.config;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Converts a platform-style {@code postgres://} / {@code postgresql://} connection URI
 * (Render, Heroku, Railway) found in {@code DATABASE_URL} into the JDBC coordinates
 * Spring Boot expects. Plain {@code jdbc:} URLs pass through untouched.
 *
 * <p>Registered in {@code META-INF/spring.factories} so it runs before the datasource
 * auto-configuration reads the environment.</p>
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String DATABASE_URL = "DATABASE_URL";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String raw = environment.getProperty(DATABASE_URL);
        if (raw == null || raw.isBlank()) {
            return;
        }

        String url = raw.trim();
        if (url.startsWith("jdbc:")) {
            return;
        }
        String scheme = url.contains("://") ? url.substring(0, url.indexOf("://")) : "";
        if (!"postgres".equals(scheme) && !"postgresql".equals(scheme)) {
            return;
        }

        URI uri = URI.create(url);
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalStateException("DATABASE_URL is not a valid PostgreSQL URI: " + url);
        }
        int port = uri.getPort() > 0 ? uri.getPort() : 5432;
        String database = uri.getPath() == null || uri.getPath().isBlank()
                ? "postgres"
                : uri.getPath().replaceFirst("^/", "");

        Map<String, Object> values = new HashMap<>();
        // sslmode=prefer keeps Render's internal (non-TLS) network working while still
        // negotiating TLS when the connection is made over the public internet.
        values.put("spring.datasource.url",
                "jdbc:postgresql://" + host + ":" + port + "/" + database + "?sslmode=prefer");

        String userInfo = uri.getUserInfo();
        if (userInfo != null && userInfo.contains(":")) {
            String[] parts = userInfo.split(":", 2);
            values.put("spring.datasource.username", URLDecoder.decode(parts[0], StandardCharsets.UTF_8));
            values.put("spring.datasource.password", URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
        }

        environment.getPropertySources().addFirst(new MapPropertySource("platform-database-url", values));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}

package com.retailnexus.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * When running on Render with a PostgreSQL database, use DATABASE_URL so data persists.
 * If DATABASE_URL is set (postgres://...), creates a PostgreSQL DataSource and sets Hibernate to
 * use PostgreSQL dialect and ddl-auto=update. Otherwise the default (H2 in-memory from
 * application-render.properties) is used.
 */
@Configuration
@Profile("render")
public class RenderDatabaseConfig {

    @Bean
    @ConditionalOnProperty(name = "DATABASE_URL")
    public DataSource dataSource(Environment env) {
        String url = env.getProperty("DATABASE_URL");
        if (url == null || url.isBlank()) return null;
        return createPostgresDataSource(url);
    }

    @Bean
    @ConditionalOnProperty(name = "DATABASE_URL")
    public HibernatePropertiesCustomizer postgresHibernateCustomizer() {
        return props -> {
            props.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
            props.put("hibernate.hbm2ddl.auto", "update");
        };
    }

    private static DataSource createPostgresDataSource(String databaseUrl) {
        try {
            if (databaseUrl.startsWith("postgres://")) {
                databaseUrl = "postgresql://" + databaseUrl.substring(11);
            }
            URI uri = new URI(databaseUrl);
            String userInfo = uri.getUserInfo();
            String username = "";
            String password = "";
            if (userInfo != null && userInfo.contains(":")) {
                username = userInfo.substring(0, userInfo.indexOf(':'));
                password = userInfo.substring(userInfo.indexOf(':') + 1);
            } else if (userInfo != null) {
                username = userInfo;
            }
            username = URLDecoder.decode(username, StandardCharsets.UTF_8);
            password = URLDecoder.decode(password, StandardCharsets.UTF_8);

            String host = uri.getHost();
            int port = uri.getPort() > 0 ? uri.getPort() : 5432;
            String path = uri.getPath();
            if (path != null && path.startsWith("/")) path = path.substring(1);
            String dbName = path != null && !path.isBlank() ? path : "retailnexus";

            String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + dbName;
            String query = uri.getQuery();
            if (query != null && !query.isBlank()) {
                jdbcUrl += "?" + query;
            } else {
                jdbcUrl += "?sslmode=require";
            }

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(jdbcUrl);
            config.setUsername(username);
            config.setPassword(password);
            config.setDriverClassName("org.postgresql.Driver");
            return new HikariDataSource(config);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create PostgreSQL DataSource from DATABASE_URL", e);
        }
    }
}

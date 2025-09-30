package org.tanzu.ipzs.legislation.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

@Configuration
@Profile("!cloud")
public class DatabaseInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    public DataSource dataSource(DataSourceProperties properties) {
        String datasourceUrl = properties.getUrl();
        String username = properties.getUsername();
        String password = properties.getPassword();

        String databaseName = extractDatabaseName(datasourceUrl);
        String serverUrl = getServerUrl(datasourceUrl);

        logger.info("Checking if database '{}' exists...", databaseName);

        try (Connection connection = DriverManager.getConnection(serverUrl, username, password);
             Statement statement = connection.createStatement()) {

            // Check if database exists
            var resultSet = statement.executeQuery(
                    "SELECT 1 FROM pg_database WHERE datname = '" + databaseName + "'");

            if (!resultSet.next()) {
                logger.info("Database '{}' does not exist. Creating...", databaseName);
                statement.executeUpdate("CREATE DATABASE " + databaseName);
                logger.info("Database '{}' created successfully", databaseName);
            } else {
                logger.info("Database '{}' already exists", databaseName);
            }

        } catch (SQLException e) {
            logger.error("Failed to create database '{}': {}", databaseName, e.getMessage());
            throw new RuntimeException("Database initialization failed", e);
        }

        return properties.initializeDataSourceBuilder().build();
    }

    private String extractDatabaseName(String url) {
        int lastSlash = url.lastIndexOf('/');
        if (lastSlash == -1) {
            throw new IllegalArgumentException("Invalid database URL format: " + url);
        }
        String dbNameWithParams = url.substring(lastSlash + 1);
        // Remove query parameters if present
        int queryStart = dbNameWithParams.indexOf('?');
        return queryStart > 0 ? dbNameWithParams.substring(0, queryStart) : dbNameWithParams;
    }

    private String getServerUrl(String url) {
        int lastSlash = url.lastIndexOf('/');
        if (lastSlash == -1) {
            throw new IllegalArgumentException("Invalid database URL format: " + url);
        }
        return url.substring(0, lastSlash + 1) + "postgres";
    }
}
package com.dbmetagen.app.config;

import lombok.Data;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Component
@Data
public class DatabaseConfig {
    private String url;
    private String username;
    private String password;
    private String modelPackage;
    private String outputDirectory;
    
    private static final String CONFIG_PATH = "src/main/resources/db-config.json";

    public DatabaseConfig() {
        loadConfiguration();
    }
    
    public void reloadConfiguration() {
        loadConfiguration();
    }
    
    private void loadConfiguration() {
        try {
            Path configPath = Paths.get(CONFIG_PATH);
            
            if (!Files.exists(configPath)) {
                throw new RuntimeException("Configuration file not found: " + CONFIG_PATH);
            }
            
            String content = new String(Files.readAllBytes(configPath));
            JSONObject jsonObject = new JSONObject(content);

            // Validate required fields
            String[] requiredFields = {"url", "username", "password", "modelPackage", "outputDirectory"};
            for (String field : requiredFields) {
                if (!jsonObject.has(field)) {
                    throw new RuntimeException("Missing required field in configuration: " + field);
                }
            }
            
            this.url = jsonObject.getString("url");
            this.username = jsonObject.getString("username");
            this.password = jsonObject.getString("password");
            this.modelPackage = jsonObject.getString("modelPackage");
            this.outputDirectory = jsonObject.getString("outputDirectory");
            
        } catch (IOException e) {
            throw new RuntimeException("Failed to read database configuration file", e);
        } catch (JSONException e) {
            throw new RuntimeException("Invalid JSON format in configuration file", e);
        }
    }
    
    /**
     * Tests the database connection with the current configuration
     * @return true if the connection was successful
     * @throws SQLException if the connection fails
     */
    public boolean testConnection() throws SQLException {
        try (Connection connection = DriverManager.getConnection(
                this.url,
                this.username,
                this.password)) {
            return connection.isValid(5); // 5 second timeout
        }
    }
} 
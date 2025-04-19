package com.dbmetagen.app.config;

import lombok.Data;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Component
@Data
public class DatabaseConfig {
    private String url;
    private String username;
    private String password;
    private String modelPackage;
    private String outputDirectory;

    public DatabaseConfig() {
        try {
            String content = new String(Files.readAllBytes(Paths.get("src/main/resources/db-config.json")));
            JSONObject jsonObject = new JSONObject(content);

            this.url = jsonObject.getString("url");
            this.username = jsonObject.getString("username");
            this.password = jsonObject.getString("password");
            this.modelPackage = jsonObject.getString("modelPackage");
            this.outputDirectory = jsonObject.getString("outputDirectory");
        } catch (IOException e) {
            throw new RuntimeException("Failed to load database configuration", e);
        }
    }
} 
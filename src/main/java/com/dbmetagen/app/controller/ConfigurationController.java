package com.dbmetagen.app.controller;

import com.dbmetagen.app.config.DatabaseConfig;
import com.dbmetagen.app.service.ModelGeneratorService;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/config")
public class ConfigurationController {

    private final DatabaseConfig databaseConfig;
    private final ModelGeneratorService modelGeneratorService;
    
    @Autowired
    public ConfigurationController(DatabaseConfig databaseConfig, ModelGeneratorService modelGeneratorService) {
        this.databaseConfig = databaseConfig;
        this.modelGeneratorService = modelGeneratorService;
    }
    
    @GetMapping("/current")
    public ResponseEntity<Map<String, String>> getCurrentConfig() {
        Map<String, String> config = new HashMap<>();
        config.put("url", databaseConfig.getUrl());
        config.put("username", databaseConfig.getUsername());
        // Don't expose password in response
        config.put("modelPackage", databaseConfig.getModelPackage());
        config.put("outputDirectory", databaseConfig.getOutputDirectory());
        
        return ResponseEntity.ok(config);
    }
    
    /**
     * Update configuration using direct JSON body instead of file upload
     */
    @PostMapping(value = "/update", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateDatabaseConfig(@RequestBody Map<String, String> configMap) {
        try {
            // Validate required fields
            String[] requiredFields = {"url", "username", "password", "modelPackage", "outputDirectory"};
            for (String field : requiredFields) {
                if (!configMap.containsKey(field) || configMap.get(field) == null || configMap.get(field).trim().isEmpty()) {
                    Map<String, Object> error = new HashMap<>();
                    error.put("status", "error");
                    error.put("message", "Missing required field: " + field);
                    return ResponseEntity.badRequest().body(error);
                }
            }
            
            // Validate database URL format
            String inputUrl = configMap.get("url");
            if (inputUrl == null || !inputUrl.startsWith("jdbc:")) {
                Map<String, Object> error = new HashMap<>();
                error.put("status", "error");
                error.put("message", "Invalid database URL format. Must start with 'jdbc:'");
                return ResponseEntity.badRequest().body(error);
            }
            
            // Create JSON object
            JSONObject jsonObject = new JSONObject();
            for (Map.Entry<String, String> entry : configMap.entrySet()) {
                jsonObject.put(entry.getKey(), entry.getValue());
            }
            
            // Write to file
            Path targetPath = Paths.get("src/main/resources/db-config.json");
            Files.writeString(targetPath, jsonObject.toString(2), StandardCharsets.UTF_8);
            
            // Reload configuration
            databaseConfig.reloadConfiguration();
            
            // Clear the cached database metadata
            modelGeneratorService.clearCache();
            
            // Generate response
            String dbUrl = databaseConfig.getUrl();
            String dbName = dbUrl.substring(dbUrl.lastIndexOf("/") + 1);
            String outputPath = databaseConfig.getOutputDirectory() + File.separator + dbName;
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Database configuration updated successfully");
            response.put("url", dbUrl);
            response.put("username", databaseConfig.getUsername());
            response.put("modelPackage", databaseConfig.getModelPackage());
            response.put("outputDirectory", databaseConfig.getOutputDirectory());
            response.put("modelOutputPath", outputPath + File.separator + databaseConfig.getModelPackage().replace('.', File.separatorChar));
            response.put("status", "success");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Failed to update configuration: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    @PostMapping("/upload")
    public ResponseEntity<?> uploadDatabaseConfig(@RequestParam("file") MultipartFile file) {
        try {
            // Check if the file is empty
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("Please upload a non-empty db-config.json file");
            }
            
            // Check if the file is a JSON file
            if (!file.getOriginalFilename().endsWith(".json")) {
                return ResponseEntity.badRequest().body("Please upload a JSON file");
            }
            
            // Validate JSON content and required fields
            String jsonContent = new String(file.getBytes(), StandardCharsets.UTF_8);
            try {
                JSONObject json = new JSONObject(jsonContent);
                
                // Check required fields
                String[] requiredFields = {"url", "username", "password", "modelPackage", "outputDirectory"};
                for (String field : requiredFields) {
                    if (!json.has(field) || json.getString(field).trim().isEmpty()) {
                        return ResponseEntity.badRequest().body("Missing required field: " + field);
                    }
                }
                
                // Validate database URL format
                String inputUrl = json.getString("url");
                if (!inputUrl.startsWith("jdbc:")) {
                    return ResponseEntity.badRequest().body("Invalid database URL format. Must start with 'jdbc:'");
                }
                
            } catch (JSONException e) {
                return ResponseEntity.badRequest().body("Invalid JSON format: " + e.getMessage());
            }
            
            // Save the file to the resources directory
            Path targetPath = Paths.get("src/main/resources/db-config.json");
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            
            // Force reload of database config
            databaseConfig.reloadConfiguration();
            
            // Clear the cached database metadata
            modelGeneratorService.clearCache();
            
            // Generate response
            String dbUrl = databaseConfig.getUrl();
            String dbName = dbUrl.substring(dbUrl.lastIndexOf("/") + 1);
            String outputPath = databaseConfig.getOutputDirectory() + File.separator + dbName;
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Database configuration updated successfully");
            response.put("url", dbUrl);
            response.put("username", databaseConfig.getUsername());
            response.put("modelPackage", databaseConfig.getModelPackage());
            response.put("outputDirectory", databaseConfig.getOutputDirectory());
            response.put("modelOutputPath", outputPath + File.separator + databaseConfig.getModelPackage().replace('.', File.separatorChar));
            response.put("status", "success");
            
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Failed to upload configuration: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @GetMapping("/test-connection")
    public ResponseEntity<?> testDatabaseConnection() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean isConnected = databaseConfig.testConnection();
            response.put("status", "success");
            response.put("connected", isConnected);
            response.put("message", "Database connection successful");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("connected", false);
            response.put("message", "Failed to connect to database: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
} 
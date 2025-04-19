package com.dbmetagen.app.controller;

import com.dbmetagen.app.config.DatabaseConfig;
import com.dbmetagen.app.model.DatabaseMetadata;
import com.dbmetagen.app.model.TableMetadata;
import com.dbmetagen.app.service.ModelGeneratorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/metadata")
public class DatabaseMetadataController {

    private final ModelGeneratorService modelGeneratorService;
    private final DatabaseConfig databaseConfig;

    @Autowired
    public DatabaseMetadataController(ModelGeneratorService modelGeneratorService, DatabaseConfig databaseConfig) {
        this.modelGeneratorService = modelGeneratorService;
        this.databaseConfig = databaseConfig;
    }

    @GetMapping("")
    public ResponseEntity<?> getDatabaseMetadata() {
        try {
            return ResponseEntity.ok(modelGeneratorService.getDatabaseMetadata());
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Failed to retrieve database metadata: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @GetMapping("/tables")
    public ResponseEntity<?> getAllTables() {
        try {
            DatabaseMetadata metadata = modelGeneratorService.getDatabaseMetadata();
            return ResponseEntity.ok(metadata.getTables());
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Failed to retrieve tables: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @GetMapping("/tables/{tableName}")
    public ResponseEntity<?> getTableMetadata(@PathVariable String tableName) {
        try {
            DatabaseMetadata metadata = modelGeneratorService.getDatabaseMetadata();
            
            Optional<TableMetadata> tableMetadata = metadata.getTables().stream()
                    .filter(table -> table.getTableName().equalsIgnoreCase(tableName))
                    .findFirst();
            
            if (tableMetadata.isPresent()) {
                return ResponseEntity.ok(tableMetadata.get());
            } else {
                Map<String, Object> error = new HashMap<>();
                error.put("status", "error");
                error.put("message", "Table not found: " + tableName);
                return ResponseEntity.status(404).body(error);
            }
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Failed to retrieve table metadata: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @GetMapping("/models")
    public ResponseEntity<?> generateAllModels() {
        try {
            Map<String, String> models = modelGeneratorService.generateModelClasses();
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("models", models);
            response.put("count", models.size());
            response.put("message", "Generated " + models.size() + " model classes");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Failed to generate models: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @GetMapping("/models/{tableName}")
    public ResponseEntity<?> generateModelForTable(@PathVariable String tableName) {
        try {
            DatabaseMetadata metadata = modelGeneratorService.getDatabaseMetadata();
            
            Optional<TableMetadata> tableMetadata = metadata.getTables().stream()
                    .filter(table -> table.getTableName().equalsIgnoreCase(tableName))
                    .findFirst();
            
            if (tableMetadata.isPresent()) {
                // Generate the model code
                String modelCode = modelGeneratorService.generateModelClass(tableMetadata.get());
                
                // Save the model to disk
                TableMetadata table = tableMetadata.get();
                String className = toClassName(table.getTableName());
                saveModelToFile(className, modelCode);
                
                // Return the response
                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("tableName", tableName);
                response.put("className", className);
                response.put("modelClass", modelCode);
                response.put("fileSaved", true);
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> error = new HashMap<>();
                error.put("status", "error");
                error.put("message", "Table not found: " + tableName);
                return ResponseEntity.status(404).body(error);
            }
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Failed to generate model: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * Helper method to convert table name to class name (PascalCase)
     */
    private String toClassName(String tableName) {
        StringBuilder result = new StringBuilder();
        
        // Split by underscore and capitalize each part
        String[] parts = tableName.split("_");
        for (String part : parts) {
            if (!part.isEmpty()) {
                result.append(Character.toUpperCase(part.charAt(0)));
                
                if (part.length() > 1) {
                    result.append(part.substring(1).toLowerCase());
                }
            }
        }
        
        return result.toString();
    }
    
    /**
     * Helper method to save a model class to disk
     */
    private void saveModelToFile(String className, String modelCode) throws IOException {
        // Get configuration from service
        String outputDirectory = databaseConfig.getOutputDirectory();
        String modelPackage = databaseConfig.getModelPackage();
        
        // Create package directory structure
        String packagePath = modelPackage.replace('.', '/');
        File outputDir = new File(outputDirectory);
        File packageDir = new File(outputDir, packagePath);
        
        // Ensure directories exist
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        if (!packageDir.exists()) {
            packageDir.mkdirs();
        }
        
        // Write the file
        File modelFile = new File(packageDir, className + ".java");
        try (FileWriter writer = new FileWriter(modelFile)) {
            writer.write(modelCode);
        }
    }
} 
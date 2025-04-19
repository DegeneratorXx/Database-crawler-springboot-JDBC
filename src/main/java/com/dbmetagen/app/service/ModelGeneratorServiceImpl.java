package com.dbmetagen.app.service;

import com.dbmetagen.app.config.DatabaseConfig;
import com.dbmetagen.app.model.ColumnMetadata;
import com.dbmetagen.app.model.DatabaseMetadata;
import com.dbmetagen.app.model.ForeignKeyMetadata;
import com.dbmetagen.app.model.TableMetadata;
import com.dbmetagen.app.repository.DatabaseMetadataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ModelGeneratorServiceImpl implements ModelGeneratorService {

    private final DatabaseMetadataRepository repository;
    private final DatabaseConfig databaseConfig;
    private DatabaseMetadata databaseMetadata;

    @Autowired
    public ModelGeneratorServiceImpl(DatabaseMetadataRepository repository, DatabaseConfig databaseConfig) {
        this.repository = repository;
        this.databaseConfig = databaseConfig;
    }

    @Override
    public DatabaseMetadata getDatabaseMetadata() {
        if (databaseMetadata == null) {
            databaseMetadata = repository.extractDatabaseMetadata();
        }
        return databaseMetadata;
    }
    
    @Override
    public void clearCache() {
        databaseMetadata = null;
    }

    @Override
    public Map<String, String> generateModelClasses() {
        DatabaseMetadata metadata = getDatabaseMetadata();
        Map<String, String> generatedModels = new HashMap<>();
        
        // Get database name for organization
        String databaseName = metadata.getDatabaseName();
        
        // Ensure base output directory exists
        File baseOutputDir = new File(databaseConfig.getOutputDirectory());
        if (!baseOutputDir.exists()) {
            baseOutputDir.mkdirs();
        }
        
        // Create database-specific output directory
        File dbOutputDir = new File(baseOutputDir, databaseName);
        if (!dbOutputDir.exists()) {
            dbOutputDir.mkdirs();
        }
        
        // Create package directory structure
        String packagePath = databaseConfig.getModelPackage().replace('.', '/');
        File packageDir = new File(dbOutputDir, packagePath);
        if (!packageDir.exists()) {
            packageDir.mkdirs();
        }
        
        // Generate model classes for each table
        for (TableMetadata table : metadata.getTables()) {
            String modelCode = generateModelClass(table);
            String className = toClassName(table.getTableName());
            generatedModels.put(className, modelCode);
            
            // Write to file
            try {
                File modelFile = new File(packageDir, className + ".java");
                FileWriter writer = new FileWriter(modelFile);
                writer.write(modelCode);
                writer.close();
            } catch (IOException e) {
                throw new RuntimeException("Failed to write model class: " + className, e);
            }
        }
        
        return generatedModels;
    }

    @Override
    public String generateModelClass(TableMetadata tableMetadata) {
        StringBuilder modelBuilder = new StringBuilder();
        
        String className = toClassName(tableMetadata.getTableName());
        
        // Package declaration
        modelBuilder.append("package ").append(databaseConfig.getModelPackage()).append(";\n\n");
        
        // Import statements
        modelBuilder.append("import java.util.*;\n");
        modelBuilder.append("import java.sql.*;\n");
        modelBuilder.append("import lombok.Data;\n\n");
        
        // Class declaration with Lombok @Data
        modelBuilder.append("/**\n");
        modelBuilder.append(" * Auto-generated model class for table: ").append(tableMetadata.getTableName()).append("\n");
        modelBuilder.append(" */\n");
        modelBuilder.append("@Data\n");
        modelBuilder.append("public class ").append(className).append(" {\n\n");
        
        // Fields
        for (ColumnMetadata column : tableMetadata.getColumns()) {
            // Field comment
            modelBuilder.append("    /**\n");
            modelBuilder.append("     * ").append(column.getColumnName()).append(" - ");
            modelBuilder.append(column.getDataType());
            
            if (column.isPrimaryKey()) {
                modelBuilder.append(" (Primary Key)");
            }
            if (column.isAutoIncrement()) {
                modelBuilder.append(" (Auto Increment)");
            }
            
            modelBuilder.append("\n");
            modelBuilder.append("     */\n");
            
            // Field declaration
            modelBuilder.append("    private ").append(getJavaType(column.getDataType()));
            modelBuilder.append(" ").append(toCamelCase(column.getColumnName())).append(";\n\n");
        }
        
        // Add relationship fields
        for (ForeignKeyMetadata fk : tableMetadata.getForeignKeys()) {
            String refTableClassName = toClassName(fk.getReferenceTableName());
            String fieldName = toCamelCase(fk.getReferenceTableName());
            
            modelBuilder.append("    /**\n");
            modelBuilder.append("     * Relationship: ").append(fk.getConstraintName()).append("\n");
            modelBuilder.append("     * Referenced table: ").append(fk.getReferenceTableName()).append("\n");
            modelBuilder.append("     */\n");
            modelBuilder.append("    private ").append(refTableClassName).append(" ").append(fieldName).append(";\n\n");
        }
        
        // Close class
        modelBuilder.append("}\n");
        
        return modelBuilder.toString();
    }
    
    // Helper method to convert table name to class name (PascalCase)
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
    
    // Helper method to convert column name to field name (camelCase)
    private String toCamelCase(String columnName) {
        StringBuilder result = new StringBuilder();
        
        // Split by underscore
        String[] parts = columnName.split("_");
        
        // First part is lowercase
        if (parts.length > 0 && !parts[0].isEmpty()) {
            result.append(parts[0].toLowerCase());
        }
        
        // Rest are capitalized
        for (int i = 1; i < parts.length; i++) {
            if (!parts[i].isEmpty()) {
                result.append(Character.toUpperCase(parts[i].charAt(0)));
                
                if (parts[i].length() > 1) {
                    result.append(parts[i].substring(1).toLowerCase());
                }
            }
        }
        
        return result.toString();
    }
    
    // Helper method to map SQL data types to Java types
    private String getJavaType(String sqlType) {
        sqlType = sqlType.toUpperCase();
        
        if (sqlType.contains("INT")) {
            return "Integer";
        } else if (sqlType.contains("FLOAT") || sqlType.contains("DOUBLE") || sqlType.contains("DECIMAL")) {
            return "Double";
        } else if (sqlType.contains("BOOLEAN") || sqlType.contains("BIT")) {
            return "Boolean";
        } else if (sqlType.contains("DATE")) {
            return "java.util.Date";
        } else if (sqlType.contains("TIME")) {
            if (sqlType.contains("TIMESTAMP")) {
                return "java.sql.Timestamp";
            }
            return "java.sql.Time";
        } else if (sqlType.contains("BLOB")) {
            return "byte[]";
        } else {
            return "String";
        }
    }
} 
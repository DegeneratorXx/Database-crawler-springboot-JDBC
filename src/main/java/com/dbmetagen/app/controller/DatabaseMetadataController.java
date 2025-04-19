package com.dbmetagen.app.controller;

import com.dbmetagen.app.model.DatabaseMetadata;
import com.dbmetagen.app.model.TableMetadata;
import com.dbmetagen.app.service.ModelGeneratorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/metadata")
public class DatabaseMetadataController {

    private final ModelGeneratorService modelGeneratorService;

    @Autowired
    public DatabaseMetadataController(ModelGeneratorService modelGeneratorService) {
        this.modelGeneratorService = modelGeneratorService;
    }

    @GetMapping("")
    public ResponseEntity<DatabaseMetadata> getDatabaseMetadata() {
        return ResponseEntity.ok(modelGeneratorService.getDatabaseMetadata());
    }

    @GetMapping("/tables")
    public ResponseEntity<List<TableMetadata>> getAllTables() {
        DatabaseMetadata metadata = modelGeneratorService.getDatabaseMetadata();
        return ResponseEntity.ok(metadata.getTables());
    }

    @GetMapping("/tables/{tableName}")
    public ResponseEntity<TableMetadata> getTableMetadata(@PathVariable String tableName) {
        DatabaseMetadata metadata = modelGeneratorService.getDatabaseMetadata();
        
        Optional<TableMetadata> tableMetadata = metadata.getTables().stream()
                .filter(table -> table.getTableName().equalsIgnoreCase(tableName))
                .findFirst();
        
        return tableMetadata.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/models")
    public ResponseEntity<Map<String, String>> generateAllModels() {
        Map<String, String> models = modelGeneratorService.generateModelClasses();
        return ResponseEntity.ok(models);
    }

    @GetMapping("/models/{tableName}")
    public ResponseEntity<String> generateModelForTable(@PathVariable String tableName) {
        DatabaseMetadata metadata = modelGeneratorService.getDatabaseMetadata();
        
        Optional<TableMetadata> tableMetadata = metadata.getTables().stream()
                .filter(table -> table.getTableName().equalsIgnoreCase(tableName))
                .findFirst();
        
        if (tableMetadata.isPresent()) {
            String modelCode = modelGeneratorService.generateModelClass(tableMetadata.get());
            return ResponseEntity.ok(modelCode);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
} 
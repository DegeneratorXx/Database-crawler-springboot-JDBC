package com.dbmetagen.app.model;

import lombok.Data;
import java.util.List;
 
@Data
public class DatabaseMetadata {
    private String databaseName;
    private List<TableMetadata> tables;
} 
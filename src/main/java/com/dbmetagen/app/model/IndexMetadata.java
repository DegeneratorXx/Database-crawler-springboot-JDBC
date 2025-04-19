package com.dbmetagen.app.model;

import lombok.Data;
import java.util.List;

@Data
public class IndexMetadata {
    private String indexName;
    private boolean isUnique;
    private List<String> columnNames;
} 
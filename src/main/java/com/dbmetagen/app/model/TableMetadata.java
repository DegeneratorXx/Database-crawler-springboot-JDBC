package com.dbmetagen.app.model;

import lombok.Data;
import java.util.List;

@Data
public class TableMetadata {
    private String tableName;
    private List<ColumnMetadata> columns;
    private List<ForeignKeyMetadata> foreignKeys;
    private List<IndexMetadata> indexes;
    private String primaryKey;
} 
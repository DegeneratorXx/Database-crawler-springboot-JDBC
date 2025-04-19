package com.dbmetagen.app.model;

import lombok.Data;

@Data
public class ColumnMetadata {
    private String columnName;
    private String dataType;
    private int size;
    private boolean isNullable;
    private boolean isPrimaryKey;
    private boolean isAutoIncrement;
    private String defaultValue;
} 
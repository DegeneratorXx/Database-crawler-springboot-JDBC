package com.dbmetagen.app.model;

import lombok.Data;

@Data
public class ForeignKeyMetadata {
    private String constraintName;
    private String columnName;
    private String referenceTableName;
    private String referenceColumnName;
} 
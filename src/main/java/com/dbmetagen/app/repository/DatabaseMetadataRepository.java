package com.dbmetagen.app.repository;

import com.dbmetagen.app.model.DatabaseMetadata;
 
public interface DatabaseMetadataRepository {
    DatabaseMetadata extractDatabaseMetadata();
} 
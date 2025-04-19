package com.dbmetagen.app.service;

import com.dbmetagen.app.model.DatabaseMetadata;
import com.dbmetagen.app.model.TableMetadata;

import java.util.Map;

public interface ModelGeneratorService {
    DatabaseMetadata getDatabaseMetadata();
    Map<String, String> generateModelClasses();
    String generateModelClass(TableMetadata tableMetadata);
} 
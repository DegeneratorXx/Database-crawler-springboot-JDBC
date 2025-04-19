package com.dbmetagen.app.repository;

import com.dbmetagen.app.config.DatabaseConfig;
import com.dbmetagen.app.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class JdbcDatabaseMetadataRepository implements DatabaseMetadataRepository {

    private final DatabaseConfig databaseConfig;

    @Autowired
    public JdbcDatabaseMetadataRepository(DatabaseConfig databaseConfig) {
        this.databaseConfig = databaseConfig;
    }

    @Override
    public DatabaseMetadata extractDatabaseMetadata() {
        DatabaseMetadata metadata = new DatabaseMetadata();
        
        try (Connection connection = DriverManager.getConnection(
                databaseConfig.getUrl(),
                databaseConfig.getUsername(),
                databaseConfig.getPassword())) {
            
            // Extract database name from connection URL
            String url = databaseConfig.getUrl();
            String dbName = url.substring(url.lastIndexOf("/") + 1);
            metadata.setDatabaseName(dbName);
            
            DatabaseMetaData metaData = connection.getMetaData();
            List<TableMetadata> tables = extractTables(metaData, dbName);
            metadata.setTables(tables);
            
            return metadata;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to extract database metadata", e);
        }
    }

    private List<TableMetadata> extractTables(DatabaseMetaData metaData, String dbName) throws SQLException {
        List<TableMetadata> tables = new ArrayList<>();
        
        try (ResultSet rs = metaData.getTables(dbName, null, "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                
                TableMetadata tableMetadata = new TableMetadata();
                tableMetadata.setTableName(tableName);
                
                // Extract columns
                tableMetadata.setColumns(extractColumns(metaData, dbName, tableName));
                
                // Extract primary keys
                extractPrimaryKeys(metaData, dbName, tableName, tableMetadata);
                
                // Extract foreign keys
                tableMetadata.setForeignKeys(extractForeignKeys(metaData, dbName, tableName));
                
                // Extract indexes
                tableMetadata.setIndexes(extractIndexes(metaData, dbName, tableName));
                
                tables.add(tableMetadata);
            }
        }
        
        return tables;
    }

    private List<ColumnMetadata> extractColumns(DatabaseMetaData metaData, String dbName, String tableName) throws SQLException {
        List<ColumnMetadata> columns = new ArrayList<>();
        
        try (ResultSet rs = metaData.getColumns(dbName, null, tableName, "%")) {
            while (rs.next()) {
                ColumnMetadata column = new ColumnMetadata();
                column.setColumnName(rs.getString("COLUMN_NAME"));
                column.setDataType(rs.getString("TYPE_NAME"));
                column.setSize(rs.getInt("COLUMN_SIZE"));
                column.setNullable(rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable);
                column.setDefaultValue(rs.getString("COLUMN_DEF"));
                column.setAutoIncrement("YES".equals(rs.getString("IS_AUTOINCREMENT")));
                
                columns.add(column);
            }
        }
        
        return columns;
    }

    private void extractPrimaryKeys(DatabaseMetaData metaData, String dbName, String tableName, TableMetadata tableMetadata) throws SQLException {
        try (ResultSet rs = metaData.getPrimaryKeys(dbName, null, tableName)) {
            if (rs.next()) {
                String pkColumnName = rs.getString("COLUMN_NAME");
                tableMetadata.setPrimaryKey(pkColumnName);
                
                // Mark column as PK
                for (ColumnMetadata column : tableMetadata.getColumns()) {
                    if (column.getColumnName().equals(pkColumnName)) {
                        column.setPrimaryKey(true);
                        break;
                    }
                }
            }
        }
    }

    private List<ForeignKeyMetadata> extractForeignKeys(DatabaseMetaData metaData, String dbName, String tableName) throws SQLException {
        List<ForeignKeyMetadata> foreignKeys = new ArrayList<>();
        
        try (ResultSet rs = metaData.getImportedKeys(dbName, null, tableName)) {
            while (rs.next()) {
                ForeignKeyMetadata fk = new ForeignKeyMetadata();
                fk.setConstraintName(rs.getString("FK_NAME"));
                fk.setColumnName(rs.getString("FKCOLUMN_NAME"));
                fk.setReferenceTableName(rs.getString("PKTABLE_NAME"));
                fk.setReferenceColumnName(rs.getString("PKCOLUMN_NAME"));
                
                foreignKeys.add(fk);
            }
        }
        
        return foreignKeys;
    }

    private List<IndexMetadata> extractIndexes(DatabaseMetaData metaData, String dbName, String tableName) throws SQLException {
        Map<String, IndexMetadata> indexMap = new HashMap<>();
        
        try (ResultSet rs = metaData.getIndexInfo(dbName, null, tableName, false, false)) {
            while (rs.next()) {
                String indexName = rs.getString("INDEX_NAME");
                boolean nonUnique = rs.getBoolean("NON_UNIQUE");
                String columnName = rs.getString("COLUMN_NAME");
                
                if (indexName == null) {
                    continue; // Skip undefined indexes
                }
                
                // Check if we already have this index
                IndexMetadata index = indexMap.get(indexName);
                if (index == null) {
                    // Create new index
                    index = new IndexMetadata();
                    index.setIndexName(indexName);
                    index.setUnique(!nonUnique);
                    index.setColumnNames(new ArrayList<>());
                    indexMap.put(indexName, index);
                }
                
                // Add column to the index
                if (columnName != null) {
                    index.getColumnNames().add(columnName);
                }
            }
        }
        
        return new ArrayList<>(indexMap.values());
    }
} 
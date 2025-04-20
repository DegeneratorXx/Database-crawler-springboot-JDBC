# Database Metadata Generator - Architecture Overview

## Overall Architecture and Technical Design

The Database Metadata Generator is designed as a Spring Boot application following a layered architecture pattern. This document outlines the architecture, components, and workflow of the application.

### Architectural Layers

```
┌─────────────────────┐
│ Controller Layer    │
│ (REST API Endpoints)│
└─────────┬───────────┘
          │
          ▼
┌─────────────────────┐
│ Service Layer       │
│ (Business Logic)    │
└─────────┬───────────┘
          │
          ▼
┌─────────────────────┐
│ Repository Layer    │
│ (Data Access)       │
└─────────┬───────────┘
          │
          ▼
┌─────────────────────┐
│ Database Connection │
└─────────────────────┘
```

#### 1. Configuration Layer
- **Purpose**: Manages database connection details and application settings
- **Key Component**: `DatabaseConfig` class
- **Responsibilities**:
  - Load configuration from JSON file (`db-config.json`)
  - Provide access to database connection parameters
  - Allow dynamic reconfiguration at runtime
  - Test database connectivity

#### 2. Repository Layer
- **Purpose**: Handles database connectivity and metadata extraction
- **Key Classes**: JDBC-based data access implementations
- **Responsibilities**:
  - Connect to MySQL database using provided credentials
  - Extract metadata (tables, columns, keys, indices)
  - Map raw database metadata to domain objects

#### 3. Service Layer
- **Purpose**: Contains business logic for model generation
- **Key Component**: `ModelGeneratorService` class
- **Responsibilities**:
  - Transform database metadata into Java model classes
  - Generate appropriate field types based on column data types
  - Handle relationships between tables (foreign keys)
  - Manage output directories and file generation
  - Cache metadata to improve performance

#### 4. Controller Layer
- **Purpose**: Exposes REST API endpoints for clients
- **Key Components**: `DatabaseMetadataController` and `ConfigurationController`
- **Responsibilities**:
  - Provide endpoints for metadata retrieval
  - Handle model generation requests
  - Manage configuration updates
  - Validate inputs and provide error handling

### Technical Design Principles

1. **Separation of Concerns**: Each layer has specific, well-defined responsibilities
2. **Dependency Injection**: Spring's DI allows for loose coupling between components
3. **Stateless Services**: Core services don't maintain state between requests
4. **Error Handling**: Comprehensive exception handling and error responses
5. **Caching**: Metadata caching improves performance for repeated operations

## Database Crawler Workflow

### 1. Database Connection Process

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│ Load config │────►│ Create JDBC │────►│ Establish   │────►│ Validate    │
│ parameters  │     │ connection  │     │ connection  │     │ connection  │
└─────────────┘     └─────────────┘     └─────────────┘     └─────────────┘
```

1. **Configuration Loading**: 
   - Application reads database connection parameters from `db-config.json`
   - Parameters include URL, username, password, and output settings

2. **JDBC Connection Creation**:
   - Creates a connection object using the JDBC driver
   - Configures connection properties (timeout, batch size, etc.)

3. **Connection Verification**:
   - Tests connection by executing a basic query
   - Validates database accessibility and credentials

### 2. Metadata Extraction Process

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│ Query DB    │────►│ Extract     │────►│ Extract     │────►│ Extract     │
│ catalog     │     │ tables      │     │ columns     │     │ keys/indices│
└─────────────┘     └─────────────┘     └─────────────┘     └─────────────┘
```

1. **Database Catalog Query**:
   - Queries the database catalog to identify available tables
   - Filters system tables and views if configured to do so

2. **Table Metadata Extraction**:
   - For each table, extracts table-level metadata (name, type, etc.)
   - Creates domain objects representing database tables

3. **Column Metadata Extraction**:
   - For each table, extracts column metadata (name, type, size, nullable)
   - Maps SQL data types to Java data types

4. **Constraint Extraction**:
   - Extracts primary key information
   - Extracts foreign key relationships between tables
   - Extracts unique constraints and indices

### 3. Model Generation Process

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│ Analyze     │────►│ Generate    │────►│ Add         │────►│ Write to    │
│ metadata    │     │ Java classes│     │ annotations │     │ files       │
└─────────────┘     └─────────────┘     └─────────────┘     └─────────────┘
```

1. **Metadata Analysis**:
   - Analyzes the extracted metadata for model generation
   - Resolves relationships between tables
   - Determines class and field naming based on conventions

2. **Java Class Generation**:
   - For each table, generates a corresponding Java class
   - Converts column names to camelCase field names
   - Maps SQL data types to appropriate Java types
   - Handles special cases (nullable types, large fields, etc.)

3. **Annotation Generation**:
   - Adds appropriate annotations to the generated classes
   - Includes meta-information about database structure
   - Marks relationships between entities (OneToMany, ManyToOne, etc.)

4. **File Output**:
   - Creates directory structure based on package name
   - Writes generated Java classes to files
   - Organizes by database name to support multiple database scenarios

### Complete End-to-End Workflow

1. **Initiation**:
   - Client makes API request to `/api/metadata/models`
   - Controller validates request and delegates to service layer

2. **Configuration**:
   - Service retrieves database connection configuration
   - Establishes database connection

3. **Metadata Extraction**:
   - Queries database for table, column, and constraint metadata
   - Creates domain model representing database structure

4. **Model Generation**:
   - Transforms database metadata into Java classes
   - Applies naming conventions and type mappings
   - Adds necessary annotations

5. **Output**:
   - Writes generated models to the configured output directory
   - Returns response with file generation details

6. **Caching**:
   - Caches extracted metadata to improve performance
   - Invalidates cache when configuration changes

## Performance Considerations

1. **Connection Pooling**: Reuses database connections to reduce overhead
2. **Metadata Caching**: Caches database structure information to minimize repeated queries
3. **Batch Processing**: Processes tables in batches for large databases
4. **Incremental Generation**: Can generate models for a single table rather than the entire database

## Security Considerations

1. **Credential Handling**: Credentials stored in config file, not exposed in API responses
2. **Input Validation**: All API inputs are validated to prevent injection attacks
3. **Error Handling**: Error messages provide information without exposing sensitive details 
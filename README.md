# Database Metadata and Model Generator

A Spring Boot application that extracts MySQL database schema information and automatically generates Java model classes with a RESTful API interface.

## Features

- Extracts database metadata (tables, columns, primary keys, foreign keys, and indexes)
- Automatically generates Java model classes for each table
- Organizes generated models by database name
- Provides REST APIs to access database structure and generated models
- Database connection testing
- Comprehensive error handling and validation
- Uses layered architecture (Controller, Service, Repository)

## Requirements

- Java 11 or later
- Maven
- MySQL database
- Postman or any REST client for API testing

## Quick Start

1. Clone the repository
2. Copy `src/main/resources/db-config.template.json` to `src/main/resources/db-config.json` and update with your database details
3. Build the project: `mvn clean package`
4. Run the application: `java -Xmx512m -jar target/db-meta-generator-1.0-SNAPSHOT.jar`
5. Use Postman or another REST client to interact with the API endpoints

## Configuration

You have two options to configure the database connection:

### Option 1: Edit the configuration file directly

Edit the `src/main/resources/db-config.json` file to set your database connection details:

```json
{
  "url": "jdbc:mysql://localhost:3306/your_database",
  "username": "your_username",
  "password": "your_password",
  "modelPackage": "com.dbmetagen.app.model",
  "outputDirectory": "generated"
}
```

### Option 2: Use the REST API with direct JSON

Send a POST request to `/api/config/update` with your configuration as JSON:

```
POST http://localhost:8080/api/config/update
Content-Type: application/json

{
  "url": "jdbc:mysql://localhost:3306/your_database",
  "username": "your_username",
  "password": "your_password",
  "modelPackage": "com.dbmetagen.app.model",
  "outputDirectory": "generated"
}
```

This is particularly useful for automation or when integrating with other systems.

## Building the Application

```
mvn clean package
```

## Running the Application

```
java -Xmx512m -jar target/db-meta-generator-1.0-SNAPSHOT.jar
```

Or using Maven:

```
mvn spring-boot:run
```

## API Endpoints

### Database Metadata

- `GET /api/metadata` - Get complete database metadata
- `GET /api/metadata/tables` - Get all tables metadata
- `GET /api/metadata/tables/{tableName}` - Get specific table metadata

### Model Generation

- `GET /api/metadata/models` - Generate model classes for all tables
- `GET /api/metadata/models/{tableName}` - Generate model class for a specific table

### Configuration

- `GET /api/config/current` - Get current database configuration (without password)
- `POST /api/config/update` - Update configuration using direct JSON body
- `GET /api/config/test-connection` - Test the current database connection

## Generated Files

The application generates Java model classes in the specified output directory with the following structure:

```
generated/
└── [database-name]/
    └── com/
        └── dbmetagen/
            └── app/
                └── model/
                    ├── Table1.java
                    ├── Table2.java
                    └── ...
```

This organization allows you to generate models for multiple databases without overwriting files. Each database's models are stored in their own subdirectory.

## Architecture

The application follows a layered architecture:

### Configuration Layer
- Reads database connection details from JSON file
- Manages configuration updates and reloading

### Repository Layer
- Connects to MySQL database using JDBC
- Extracts database metadata (tables, columns, keys, etc.)

### Service Layer
- Converts database metadata into Java model classes
- Manages file generation and organization

### Controller Layer
- Exposes REST APIs for accessing metadata and models
- Handles configuration management and database connections
- Provides validation and error handling

## Testing

You can test the API using any REST client like Postman or curl:

```
curl http://localhost:8080/api/metadata
```

Or to generate all models:

```
curl http://localhost:8080/api/metadata/models
```

Or to test the database connection:

```
curl http://localhost:8080/api/config/test-connection
```

## Security Considerations

- The application stores database credentials in plain text in the configuration file
- In a production environment, consider using environment variables or a secure vault
- The current implementation is designed for development/local usage

## Troubleshooting

### Memory Issues
If you encounter memory issues, you can adjust the JVM heap size:
```
java -Xmx512m -jar target/db-meta-generator-1.0-SNAPSHOT.jar
```

### Connection Problems
If you're having trouble connecting to your database:
1. Use the connection test endpoint
2. Check your database URL format (should be jdbc:mysql://host:port/dbname)
3. Verify username and password
4. Ensure your database server is running and accessible

### File Generation Issues
If models aren't being generated properly:
1. Check the output directory permissions
2. Verify your database contains tables
3. Ensure the database name is correctly specified in the URL

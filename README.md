# Database Metadata and Model Generator

A Spring Boot application that extracts MySQL database schema information and automatically generates Java model classes.

## Features

- Extracts database metadata (tables, columns, primary keys, foreign keys, and indexes)
- Automatically generates Java model classes for each table
- Provides REST APIs to access database structure and generated models
- Uses layered architecture (Controller, Service, Repository)
- Web UI for uploading configuration and testing database connections
- Comprehensive error handling and validation
- Automatic database structure detection
- Java model class generation with proper field types and relationships

## Requirements

- Java 11 or later
- Maven
- MySQL database

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

### Option 2: Upload a custom configuration file

The application provides a web interface to upload your own configuration file. After starting the application:

1. Open a web browser and navigate to `http://localhost:8080`
2. Use the upload form to submit your custom `db-config.json` file
3. The application will immediately switch to using the new configuration

This is particularly useful when you want to connect to different databases without restarting the application.

## Building the Application

```
mvn clean package
```

## Running the Application

```
java -jar target/db-meta-generator-1.0-SNAPSHOT.jar
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
- `POST /api/config/upload` - Upload custom database configuration file
- `POST /api/config/update` - Update configuration using direct JSON body
- `GET /api/config/test-connection` - Test the current database connection

## Generated Files

The application generates Java model classes in the specified output directory with the following structure:

```
generated/
└── com/
    └── dbmetagen/
        └── app/
            └── model/
                ├── Table1.java
                ├── Table2.java
                └── ...
```

## Architecture

- **Configuration Layer**: Reads database connection details from JSON file
- **Repository Layer**: Connects to MySQL and extracts metadata
- **Service Layer**: Converts metadata into Java model classes
- **Controller Layer**: Exposes REST APIs for accessing data

## Testing

You can test the API using any REST client like Postman or curl:

```
curl http://localhost:8080/api/metadata
```

Or to generate all models:

```
curl http://localhost:8080/api/metadata/models
``` 
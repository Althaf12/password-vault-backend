# Password Vault Backend

A Java Spring Boot backend application with a clean package structure for managing password vaults.

## Project Structure

```
src/main/java/com/passwordvault/backend/
├── controller/         # REST API controllers
├── dto/               # Data Transfer Objects
├── model/             # JPA Entity classes
├── repository/        # Spring Data JPA repositories
├── service/           # Business logic layer
└── PasswordVaultBackendApplication.java  # Main application class
```

## Technology Stack

- **Java**: 17
- **Spring Boot**: 3.2.1
- **Spring Data JPA**: For database operations
- **H2 Database**: In-memory database for development
- **Maven**: Build and dependency management

## Package Description

### Controller
Contains REST API endpoints for handling HTTP requests. Example: `UserController` provides CRUD operations for users.

### Model
Contains JPA entity classes that map to database tables. Example: `User` entity.

### DTO (Data Transfer Objects)
Contains objects used for transferring data between layers. Example: `UserDTO` for API responses.

### Repository
Contains Spring Data JPA repository interfaces for database access. Example: `UserRepository` extends `JpaRepository`.

### Service
Contains business logic and service layer. Example: `UserService` handles user-related operations.

## Getting Started

### Prerequisites
- Java 17 or higher
- Maven 3.6 or higher

### Build the Project
```bash
mvn clean compile
```

### Run Tests
```bash
mvn test
```

### Run the Application
```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### API Endpoints

- `GET /api/users` - Get all users
- `GET /api/users/{id}` - Get user by ID
- `POST /api/users` - Create a new user
- `PUT /api/users/{id}` - Update user
- `DELETE /api/users/{id}` - Delete user

### H2 Console

Access the H2 database console at: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:passwordvault`
- Username: `sa`
- Password: (leave empty)

## Configuration

Application configuration can be found in `src/main/resources/application.properties`
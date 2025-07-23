# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Essential Commands

### Running the Application
```bash
# Standard run
./gradlew bootRun

# Run with specific profile (local, dev, test, docker-keycloak)
./gradlew bootRun --args='--spring.profiles.active=local'

# Build and run with debugging
./run.sh
```

### Testing
```bash
# Run all tests except UI tests
./gradlew test

# Run UI tests only
./gradlew uiTest

# Run a specific test class
./gradlew test --tests TestClassName

# Run a specific test method
./gradlew test --tests TestClassName.methodName
```

### Build
```bash
# Build JAR
./gradlew bootJar

# Check dependency updates
./gradlew dependencyUpdates
```

## Architecture Overview

This is a Spring Boot demo application showcasing the [Spring User Framework](https://github.com/devondragon/SpringUserFramework). It implements a complete user management system with authentication, authorization, and user lifecycle management.

### Key Architectural Patterns

1. **MVC with Service-Repository Pattern**: Controllers delegate to services, which use repositories for data access. The framework provides base services that are extended here.

2. **Event-Driven Extension**: The demo extends the user framework by adding an Event management system, showing how to build on top of the framework's user management.

3. **Security Architecture**: 
   - Spring Security with form-based and OAuth2/OIDC authentication
   - Role-based access control with hierarchical roles
   - Audit logging for security events in separate log file

4. **Testing Strategy**:
   - Unit tests for individual components
   - Integration tests using `@IntegrationTest` annotation (combines Spring Boot test setup)
   - UI tests with Selenide for end-to-end testing
   - API tests using MockMvc for REST endpoints

### Important Conventions

1. **No Custom User Entity**: This demo uses the framework's User entity directly. Custom user data goes in separate entities (like UserProfile).

2. **Configuration Profiles**: 
   - `local`: Development with local database
   - `test`: Integration testing with H2
   - `docker-keycloak`: OIDC integration with Keycloak

3. **Template Organization**: All Thymeleaf templates are in `src/main/resources/templates/` with subdirectories for user management (`email/`, `password/`, etc.)

4. **Test Data Builders**: Use the builder classes in `src/test/java/com/devondragon/springdemo/test/data/` for consistent test data creation.

### Framework Integration Points

The application demonstrates framework usage through:
- Custom controllers that extend framework functionality (EventController)
- Service extensions (CustomUserService extends UserService)
- Configuration of framework components via application.yml
- Event listeners for user lifecycle events

When modifying user-related functionality, check if the Spring User Framework already provides it before implementing custom solutions.
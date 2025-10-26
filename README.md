# Spring User Framework Demo Application

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java Version](https://img.shields.io/badge/Java-17%2B-brightgreen)](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.6-green)](https://spring.io/projects/spring-boot)
[![Gradle](https://img.shields.io/badge/Gradle-8.0%2B-blue)](https://gradle.org/)
[![Docker](https://img.shields.io/badge/Docker-Supported-blue)](https://www.docker.com/)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](contributing)
[![Documentation](https://img.shields.io/badge/docs-comprehensive-green)](README.md)

A comprehensive demonstration application for the [Spring User Framework](https://github.com/devondragon/SpringUserFramework), showcasing how to implement user management features in a Spring Boot web application.

![Spring User Framework Demo Screenshot](/docs/images/Register.jpeg)

## Table of Contents
- [Overview](#overview)
- [Features](#features)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Testing](#testing)
- [Configuration](#configuration)
- [Project Structure](#project-structure)
- [Running the Application](#running-the-application)
- [Development Tools](#development-tools)
- [API Documentation](#api-documentation)
- [Architecture](#architecture)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)
- [Notes](#notes)


## Overview

This demo application serves as a reference implementation of the [Spring User Framework](https://github.com/devondragon/SpringUserFramework), showing how to integrate user management features into a real-world Spring Boot application. It includes a complete user interface built with Bootstrap, Thymeleaf templates, and JavaScript.

The application implements an event management system where users can browse, register for, and manage events. This demonstrates how to build application-specific functionality on top of the user management framework.

## Features

- **User Management**
  - Registration with email verification
  - Login/logout functionality
  - Password reset workflow
  - User profile management
  - Account deletion/disabling

- **Authentication & Security**
  - Username/password authentication
  - OAuth2 login with Google, Facebook, and Keycloak
  - Role-based access control
  - CSRF protection
  - Security audit logging

- **Application-Specific Features**
  - Custom user profile with additional fields
  - Event listing and management
  - User-to-event registration
  - Role-based permissions for events

- **Technical Features**
  - Spring Boot auto-configuration
  - Thymeleaf templating with fragments
  - REST API with JSON responses
  - Responsive Bootstrap UI
  - Docker integration

## Prerequisites

Before you begin, ensure you have the following installed:

- **Java**: JDK 17 or higher ([Download](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html))
- **Database**: MariaDB, MySQL, or Docker for containerized database
- **Build Tool**: Gradle (included via wrapper) or Maven
- **Optional**: Docker and Docker Compose for containerized setup
- **Git**: For cloning the repository

### System Requirements
- **Memory**: Minimum 2GB RAM (4GB recommended)
- **Disk Space**: At least 1GB free space
- **Network**: Internet connection for downloading dependencies

## Quick Start

### 🚀 Zero to Running in 5 Minutes (Docker)

The fastest way to get started is using Docker Compose:

```bash
# Clone and start everything
git clone https://github.com/devondragon/SpringUserFrameworkDemoApp.git
cd SpringUserFrameworkDemoApp
docker-compose up --build
```

**Access the Application**: `http://localhost:8080`

### Manual Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/devondragon/SpringUserFrameworkDemoApp.git
   cd SpringUserFrameworkDemoApp
   ```

2. **Set up the database** (using Docker)
   ```bash
   docker run -d --name springuser-db \
     -e MYSQL_ROOT_PASSWORD=root \
     -e MYSQL_DATABASE=springuser \
     -e MYSQL_USER=springuser \
     -e MYSQL_PASSWORD=springuser \
     -p 3306:3306 \
     mariadb:latest
   ```

3. **Configure the application**
   Copy the example configuration:
   ```bash
   cp src/main/resources/application-local.yml-example src/main/resources/application-local.yml
   ```

      (Optional for Keycloak) Copy the Keycloak configuration:
    ```bash
     cp src/main/resources/application-docker-keycloak.yml-example src/main/resources/application-docker-keycloak.yml
     ```
   Then edit the copied file as needed.

4. **Run the application**

   Choose one of the following:
     - Using Gradle:
       ```bash
       ./gradlew bootRun
       ```
     - Using Maven:
       ```bash
       mvn spring-boot:run
       ```
     - Using Docker Compose with Keycloak stack:
       ```bash
       docker-compose -f docker-compose-keycloak.yml up --build
       ```

5. **Access the Application**
   Open your browser and navigate to:
   `http://localhost:8080`

6. **Access Keycloak if enabled in Docker compose stack**
   Open your browser and navigate to:
   `https://localhost:8443`

### First Time Setup

After starting the application, you can:
- Register a new account at `http://localhost:8080/user/register`
- Use the demo data that may be pre-loaded
- Check logs for any setup issues in the console output

---

## Testing

This project includes comprehensive testing with multiple approaches:

### Running Tests

```bash
# Run all tests except UI tests
./gradlew test

# Run UI tests only (requires running application)
./gradlew uiTest

# Run specific test class
./gradlew test --tests UserApiTest

# Run specific test method
./gradlew test --tests UserApiTest.testUserRegistration
```

### Test Categories

- **Unit Tests**: Fast tests for individual components
- **Integration Tests**: Tests using `@IntegrationTest` with Spring context
- **API Tests**: REST endpoint testing with MockMvc
- **UI Tests**: End-to-end testing with Selenide
- **Security Tests**: Authentication and authorization testing

### Test Data

Test data builders are available in `src/test/java/com/digitalsanctuary/spring/demo/test/data/` for consistent test data creation.

### Test Profiles

Tests run with the `test` profile using H2 in-memory database for isolation.

---

## API Documentation

The application provides REST API endpoints for user management and event operations:

### User Management API

| Endpoint           | Method | Description     | Authentication |
| ------------------ | ------ | --------------- | -------------- |
| `/api/users`       | GET    | List all users  | Admin          |
| `/api/users/{id}`  | GET    | Get user by ID  | User/Admin     |
| `/api/users`       | POST   | Create new user | Public         |
| `/api/users/{id}`  | PUT    | Update user     | User/Admin     |
| `/api/users/{id}`  | DELETE | Delete user     | User/Admin     |
| `/api/auth/login`  | POST   | User login      | Public         |
| `/api/auth/logout` | POST   | User logout     | Authenticated  |

### Event Management API

| Endpoint                    | Method | Description        | Authentication |
| --------------------------- | ------ | ------------------ | -------------- |
| `/api/events`               | GET    | List events        | Public         |
| `/api/events/{id}`          | GET    | Get event details  | Public         |
| `/api/events`               | POST   | Create event       | Admin          |
| `/api/events/{id}/register` | POST   | Register for event | User           |

### Response Format

All API endpoints return JSON responses:

```json
{
  "success": true,
  "data": { ... },
  "message": "Operation successful",
  "errors": []
}
```

For detailed API documentation, start the application and visit `/swagger-ui.html` (if Swagger is enabled).

---

## Project Structure

```
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/digitalsanctuary/spring/demo/
    │   │       ├── controller/            # Page controllers
    │   │       ├── event/                 # Event-related functionality
    │   │       ├── user/
    │   │       │   └── profile/           # User profile extensions
    │   │       └── util/                  # Utility classes
    │   └── resources/
    │       ├── static/                    # Static resources (CSS, JS)
    │       ├── templates/                 # Thymeleaf templates
    │       │   ├── fragments/             # Reusable template fragments
    │       │   ├── mail/                  # Email templates
    │       │   └── user/                  # User management templates
    │       └── application.yml            # Application configuration
    └── test/                              # Test classes
```


## Configuration

### Configuration Profiles

The application supports multiple configuration profiles:

| Profile           | Purpose              | Database           | Use Case                             |
| ----------------- | -------------------- | ------------------ | ------------------------------------ |
| `local`           | Local development    | MariaDB/MySQL      | Development with persistent database |
| `test`            | Testing              | H2 (in-memory)     | Automated testing                    |
| `dev`             | Development server   | MariaDB/MySQL      | Shared development environment       |
| `docker-keycloak` | Docker with Keycloak | MariaDB + Keycloak | OIDC authentication testing          |

### Quick Configuration Setup

1. **Copy example configurations:**
   ```bash
   cp src/main/resources/application-local.yml-example src/main/resources/application-local.yml
   cp src/main/resources/application-docker-keycloak.yml-example src/main/resources/application-docker-keycloak.yml
   ```

2. **Edit configuration files** to match your environment
3. **Set active profile:** `--spring.profiles.active=local`

### Essential Configuration Settings

#### **Database Configuration**
The demo uses MariaDB as the default database. You can quickly spin up a MariaDB instance using Docker:
```bash
docker run -p 127.0.0.1:3306:3306 --name springuserframework \
  -e MARIADB_ROOT_PASSWORD=springuserroot \
  -e MARIADB_DATABASE=springuser \
  -e MARIADB_USER=springuser \
  -e MARIADB_PASSWORD=springuser \
  -d mariadb:latest
```

If you're running the application in a production-like environment, ensure you set the appropriate database properties in `application.yml` or your active profile.

---

#### **Mail Sending (SMTP)**
The application requires an SMTP server for sending emails (e.g., account verification and password reset). Update the SMTP settings in your configuration file:
```yaml
spring:
  mail:
    host: smtp.example.com
    port: 587
    username: your-username
    password: your-password
    properties:
      mail.smtp.auth: true
      mail.smtp.starttls.enable: true

user:
  mail:
    fromAddress: noreply@yourdomain.com
```

For local testing, the Docker Compose configuration includes a mail server that captures all outgoing emails.

---

#### **SSO OAuth2 with Google and Facebook**
To enable SSO:
1. Create OAuth credentials in Google and Facebook developer consoles.
2. Update your `application.yml`:
   ```yaml
   spring:
     security:
       oauth2:
         client:
           registration:
             google:
               client-id: YOUR_GOOGLE_CLIENT_ID
               client-secret: YOUR_GOOGLE_CLIENT_SECRET
               redirect-uri: "{baseUrl}/login/oauth2/code/google"
             facebook:
               client-id: YOUR_FACEBOOK_CLIENT_ID
               client-secret: YOUR_FACEBOOK_CLIENT_SECRET
               redirect-uri: "{baseUrl}/login/oauth2/code/facebook"
   ```

3. Use a tool like [ngrok](https://ngrok.com/) for local testing of OAuth callbacks:
   ```bash
   ngrok http 8080
   ```

Then update your OAuth2 providers' callback URLs to use the ngrok domain.

### Environment Variables

For production deployments, use environment variables instead of hardcoding values:

```bash
# Database
export SPRING_DATASOURCE_URL=jdbc:mariadb://localhost:3306/springuser
export SPRING_DATASOURCE_USERNAME=springuser
export SPRING_DATASOURCE_PASSWORD=springuser

# Mail
export SPRING_MAIL_HOST=smtp.gmail.com
export SPRING_MAIL_USERNAME=your-email@gmail.com
export SPRING_MAIL_PASSWORD=your-app-password

# OAuth2
export GOOGLE_CLIENT_ID=your-google-client-id
export GOOGLE_CLIENT_SECRET=your-google-client-secret
export FACEBOOK_CLIENT_ID=your-facebook-client-id
export FACEBOOK_CLIENT_SECRET=your-facebook-client-secret

# Security
export SPRING_SECURITY_BCRYPT_STRENGTH=12
export SPRING_SECURITY_FAILED_LOGIN_ATTEMPTS=5
```

### Important Security Settings

- **BCrypt Strength**: Set to `12` or higher for production
- **Session Timeout**: Default `30m`, adjust based on security requirements
- **Account Lockout**: Configure failed login attempts and lockout duration
- **CSRF Protection**: Enabled by default, ensure proper configuration for APIs

### Framework-Specific Configuration

See [CONFIG.md](CONFIG.md) for detailed framework configuration options or refer to the [Spring User Framework documentation](https://github.com/devondragon/SpringUserFramework) for complete configuration reference.




#### **SSO OIDC with Keycloak**
To enable SSO:
1. Create OIDC client in Keycloak admin console.
2. Update your `application-docker-keycloak.yml`:
   ```yaml
   spring:
     security:
       oauth2:
         client:
            registration:
              keycloak:
                client-id: ${DS_SPRING_USER_KEYCLOAK_CLIENT_ID} # Keycloak client ID for OAuth2
                client-secret: ${DS_SPRING_USER_KEYCLOAK_CLIENT_SECRET} # Keycloak client secret for OAuth2
                authorization-grant-type: authorization_code # Authorization grant type for OAuth2
                scope:
                  - email # Request email scope for OAuth2
                  - profile # Request profile scope for OAuth2
                  - openid # Request oidc scope for OAuth2
                client-name: Keycloak # Name of the OAuth2 client
                provider: keycloak
            provider:
              keycloak: # https://www.keycloak.org/securing-apps/oidc-layers
                issuer-uri: ${DS_SPRING_USER_KEYCLOAK_PROVIDER_ISSUER_URI}
                authorization-uri: ${DS_SPRING_USER_KEYCLOAK_PROVIDER_AUTHORIZATION_URI}
                token-uri: ${DS_SPRING_USER_KEYCLOAK_PROVIDER_TOKEN_URI}
                user-info-uri: ${DS_SPRING_USER_KEYCLOAK_PROVIDER_USER_INFO_URI}
                user-name-attribute: preferred_username # https://www.keycloak.org/docs-api/latest/rest-api/index.html#UserRepresentation
                jwk-set-uri: ${DS_SPRING_USER_KEYCLOAK_PROVIDER_JWK_SET_URI}
   ```
3. Refer to `keycloak.env` for default values for the above environment variables
4. You can directly start with Keycloak using the default realm provided in this project under `keycloak/realm/realm-export.json` that comes pre configured with a OIDC client and secret for this application Keycloak

---


## Running the Application

### Running Locally

#### Using Gradle
```bash
./gradlew bootRun
```

#### Using Maven
```bash
mvn spring-boot:run
```

#### With specific profile
```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### Running with Docker

The project includes a complete Docker setup with the application, MariaDB database, and a mail server.

```bash
docker-compose up --build
```

To launch the Keycloak stack:
```bash
docker-compose -f docker-compose-keycloak.yml up --build
```

**Note**: Test emails sent from the local Postfix server may not be accepted by all email providers. Use a real SMTP server for production use.

---

## Development Tools

### IDE Setup

**IntelliJ IDEA (Recommended):**
```bash
# Import as Gradle project
# Enable annotation processing: Settings > Build > Compiler > Annotation Processors
# Install Lombok plugin if needed
```

**VS Code:**
```bash
# Install extensions:
# - Extension Pack for Java
# - Spring Boot Extension Pack
# - Gradle for Java
```

### Common Development Tasks

```bash
# Quick development startup
./gradlew bootRun --args='--spring.profiles.active=local'

# Debug mode (port 5005)
./gradlew bootRun --debug-jvm

# Build and run with custom script
./run.sh

# Hot reload with DevTools (automatic)
# Just save files and changes will be picked up

# Check for security vulnerabilities
./gradlew dependencyCheckAnalyze

# Generate test reports
./gradlew test jacocoTestReport
```

### Performance and Monitoring

- **Application Metrics**: `/actuator/metrics`
- **Health Check**: `/actuator/health`
- **Database Console**: `/h2-console` (when using H2)
- **Log Levels**: Configure in `application.yml` or via `/actuator/loggers`

### Debugging Tips

1. **Database Issues**: Enable SQL logging with `spring.jpa.show-sql=true`
2. **Authentication Problems**: Enable security debug logging
3. **Email Issues**: Check `logs/audit.log` for user events
4. **Performance**: Use `/actuator/httptrace` to monitor requests

### Spring Boot DevTools
This project supports **Spring Boot DevTools** for live reload and auto-restart. If you are working with HTTPS locally, follow these steps to enable live reload:
1. Set the following property in `application.yml`:
   ```yaml
   spring.devtools.livereload.https=true
   ```

   Or when using Keycloak stack set the following property in `application-docker-keycloak.yml`:
   ```yaml
   spring.devtools.livereload.https=true
   ```

2. Use a reverse proxy like mitmproxy for HTTPS traffic interception:
   ```bash
   mitmproxy --mode reverse:http://localhost:35729 -p 35739
   ```

#### Resources for Live Reload:
- [Spring Boot Live Reload](https://www.digitalsanctuary.com/java/springboot-devtools-auto-restart-and-live-reload.html)
- [HTTPS Live Reload Setup](https://www.digitalsanctuary.com/java/how-to-get-springboot-livereload-working-over-https.html)

---

## Architecture

### System Overview

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Web Browser   │    │   Load Balancer  │    │   Application   │
│                 │◄──►│    (Optional)    │◄──►│   Spring Boot   │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                                                          │
                                                          ▼
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│  OAuth2 Providers│    │  Email Service   │    │    Database     │
│ Google/Facebook │◄──►│     SMTP        │◄──►│  MariaDB/MySQL  │
│   /Keycloak     │    │                 │    │                │
└─────────────────┘    └──────────────────┘    └─────────────────┘
```

### Key Architectural Patterns

1. **MVC Pattern**: Controllers handle HTTP requests, delegate to services
2. **Service Layer**: Business logic separation with framework extension
3. **Repository Pattern**: Data access abstraction through Spring Data JPA
4. **Event-Driven**: Application events for user lifecycle management
5. **Security Layered**: Spring Security with multiple authentication methods

### Technology Stack

| Layer          | Technology                  | Purpose                                        |
| -------------- | --------------------------- | ---------------------------------------------- |
| **Frontend**   | Thymeleaf + Bootstrap       | Server-side rendering with responsive UI       |
| **Backend**    | Spring Boot 3.4+            | Application framework and dependency injection |
| **Security**   | Spring Security             | Authentication, authorization, CSRF protection |
| **Data**       | Spring Data JPA + Hibernate | Object-relational mapping and data access      |
| **Database**   | MariaDB/MySQL               | Primary data persistence                       |
| **Testing**    | JUnit 5 + Selenide          | Unit, integration, and UI testing              |
| **Build**      | Gradle                      | Dependency management and build automation     |
| **Containers** | Docker + Docker Compose     | Development and deployment                     |

---

## Troubleshooting

### Common Issues and Solutions

#### Database Connection Issues
**Problem**: `Connection refused` or `Access denied`
```
Solution:
1. Verify database is running: docker ps
2. Check credentials in application-local.yml
3. Ensure database exists: SHOW DATABASES;
4. Check firewall/network connectivity
```

#### Build Failures
**Problem**: `Could not resolve dependencies`
```
Solution:
1. ./gradlew clean build --refresh-dependencies
2. Check internet connection
3. Verify Java version: java -version (requires JDK 17+)
4. Clear Gradle cache: rm -rf ~/.gradle/caches
```

#### OAuth2/OIDC Issues
**Problem**: OAuth2 login fails or redirects incorrectly
```
Solution:
1. Verify OAuth2 client credentials in application.yml
2. Check redirect URI configuration in OAuth provider
3. Use ngrok for local HTTPS testing
4. Verify Keycloak realm and client settings
```

#### Email Not Sending
**Problem**: Registration emails not received
```
Solution:
1. Check SMTP configuration in application.yml
2. Verify mail server credentials
3. Check spam/junk folders
4. Use Docker mail server for testing: docker-compose logs mailserver
```

#### Application Won't Start
**Problem**: Port conflicts or configuration errors
```
Solution:
1. Check if port 8080 is in use: lsof -i :8080
2. Change server.port in application.yml
3. Review application logs for configuration errors
4. Verify all required environment variables are set
```

### Getting Help

- **Logs**: Check console output and log files in `logs/` directory
- **Health Check**: Visit `/actuator/health` when application is running
- **Documentation**: Review [Spring User Framework docs](https://github.com/devondragon/SpringUserFramework)
- **Issues**: Report bugs on [GitHub Issues](https://github.com/devondragon/SpringUserFrameworkDemoApp/issues)

---

## Contributing

We welcome contributions! Here's how to get started:

### Development Setup

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/amazing-feature`
3. Set up development environment following the Quick Start guide
4. Make your changes following the existing code patterns

### Code Standards

- Follow existing code formatting and conventions
- Write tests for new functionality
- Update documentation as needed
- Ensure all tests pass: `./gradlew test`

### Submitting Changes

1. Commit your changes: `git commit -m "Add amazing feature"`
2. Push to your fork: `git push origin feature/amazing-feature`
3. Create a Pull Request with description of changes

### Development Commands

```bash
# Run with auto-restart
./gradlew bootRun

# Run specific test profile
./gradlew bootRun --args='--spring.profiles.active=test'

# Check for dependency updates
./gradlew dependencyUpdates

# Build without tests (faster)
./gradlew build -x test
```

---

## Notes

- This demo is based on the principles outlined in the [Baeldung Spring Security Course](https://www.baeldung.com/learn-spring-security-course).
- Feel free to customize and extend the provided functionality to suit your needs.
**Disclaimer:** This is a demo project provided as-is with no guarantees of performance, security, or production readiness.


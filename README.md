# Spring User Framework Demo Application

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java Version](https://img.shields.io/badge/Java-17%2B-brightgreen)](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.3-green)](https://spring.io/projects/spring-boot)

A comprehensive demonstration application for the [Spring User Framework](https://github.com/devondragon/SpringUserFramework), showcasing how to implement user management features in a Spring Boot web application.

![Spring User Framework Demo Screenshot](/docs/images/Register.jpeg)

## Table of Contents
- [Spring User Framework Demo Application](#spring-user-framework-demo-application)
  - [Table of Contents](#table-of-contents)
  - [Overview](#overview)
  - [Features Demonstrated](#features-demonstrated)
  - [Quick Start](#quick-start)
    - [Prerequisites](#prerequisites)
    - [Steps](#steps)
  - [Project Structure](#project-structure)
  - [Setup Guide](#setup-guide)
    - [Database Setup](#database-setup)
    - [Email Configuration](#email-configuration)
    - [OAuth2 Configuration](#oauth2-configuration)
  - [Running the Application](#running-the-application)
    - [Running Locally](#running-locally)
      - [Using Gradle](#using-gradle)
      - [Using Maven](#using-maven)
      - [With specific profile](#with-specific-profile)
    - [Running with Docker](#running-with-docker)
  - [API Documentation](#api-documentation)
  - [Customization Examples](#customization-examples)
    - [Custom User Profile](#custom-user-profile)
    - [Extending with Application Features](#extending-with-application-features)
  - [Testing](#testing)
    - [Unit Tests](#unit-tests)
    - [UI Tests](#ui-tests)
  - [Contributing](#contributing)
  - [License](#license)

## Overview

This demo application serves as a reference implementation of the [Spring User Framework](https://github.com/devondragon/SpringUserFramework), showing how to integrate user management features into a real-world Spring Boot application. It includes a complete user interface built with Bootstrap, Thymeleaf templates, and JavaScript.

The application implements an event management system where users can browse, register for, and manage events. This demonstrates how to build application-specific functionality on top of the user management framework.

## Features Demonstrated

- **User Management**
  - Registration with email verification
  - Login/logout functionality
  - Password reset workflow
  - User profile management
  - Account deletion/disabling

- **Authentication & Security**
  - Username/password authentication
  - OAuth2 login with Google and Facebook
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

## Quick Start

### Prerequisites
- JDK 17 or higher
- Maven or Gradle
- MariaDB or MySQL (or Docker for containerized database)

### Steps

1. **Clone the repository**
   ```bash
   git clone https://github.com/devondragon/SpringUserFrameworkDemoApp.git
   cd SpringUserFrameworkDemoApp
   ```

2. **Set up the database**

   Using Docker:
   ```bash
   docker run -d --name springuser-db \
     -e MYSQL_ROOT_PASSWORD=root \
     -e MYSQL_DATABASE=springuser \
     -e MYSQL_USER=springuser \
     -e MYSQL_PASSWORD=springuser \
     -p 3306:3306 \
     mariadb:latest
   ```

3. **Configure application**

   Copy the example configuration:
   ```bash
   cp src/main/resources/application-local.yml-example src/main/resources/application-local.yml
   ```

   Edit the file to update database credentials and other settings as needed.

4. **Run the application**

   Using Gradle:
   ```bash
   ./gradlew bootRun
   ```

   Using Maven:
   ```bash
   mvn spring-boot:run
   ```

5. **Access the application**

   Open your browser and visit http://localhost:8080

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

## Setup Guide

### Database Setup

The application uses MariaDB or MySQL by default. Configure the database connection in `application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mariadb://localhost:3306/springuser?createDatabaseIfNotExist=true
    username: springuser
    password: springuser
    driver-class-name: org.mariadb.jdbc.Driver
```

Database schema creation is handled automatically by Hibernate with the `ddl-auto: update` setting.

### Email Configuration

Email functionality is used for verification emails and password reset links. Configure your SMTP server:

```yaml
spring:
  mail:
    host: smtp.example.com
    port: 587
    username: your-username
    password: your-password
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
            required: true

user:
  mail:
    fromAddress: noreply@yourdomain.com
```

For local testing, the Docker Compose configuration includes a mail server that captures all outgoing emails.

### OAuth2 Configuration

To enable OAuth2 login with Google and Facebook:

1. Create OAuth 2.0 clients in the Google and Facebook developer consoles

2. Configure the credentials in `application.yml`:

```yaml
spring:
  security:
    oauth2:
      enabled: true
      client:
        registration:
          google:
            client-id: your-google-client-id
            client-secret: your-google-client-secret
            scope:
              - email
              - profile
          facebook:
            client-id: your-facebook-client-id
            client-secret: your-facebook-client-secret
            scope:
              - email
              - public_profile
```

3. For local testing with OAuth2 callbacks, use a service like [ngrok](https://ngrok.com/) to create a tunnel to your local server:

```bash
ngrok http 8080
```

Then update your OAuth2 providers' callback URLs to use the ngrok domain.

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

This will start:
- The Spring Boot application on port 8080
- MariaDB database on port 3306
- Mail server capturing all outgoing emails

## API Documentation

The application includes a Swagger UI for API documentation. After starting the application, visit:

```
http://localhost:8080/swagger-ui.html
```

This provides interactive documentation for all REST endpoints.

## Customization Examples

### Custom User Profile

The demo implements a custom user profile with additional fields:

```java
@Entity
@Table(name = "demo_user_profile")
public class DemoUserProfile extends BaseUserProfile {
    private String favoriteColor;
    private boolean receiveNewsletter;

    @OneToMany(mappedBy = "userProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EventRegistration> eventRegistrations = new ArrayList<>();

    // Methods for managing event registrations
    public void addEventRegistration(EventRegistration registration) {
        eventRegistrations.add(registration);
        registration.setUserProfile(this);
    }

    // ...additional methods...
}
```

### Extending with Application Features

The Events feature demonstrates how to build application functionality on top of the user framework:

```java
@Controller
public class EventPageController {
    // Controllers for event-related pages
}

@RestController
@RequestMapping("/api/events")
public class EventAPIController {
    // REST API for event management
}
```

## Testing

NOTE: The tests are not yet complete and are a work in progress.

The project includes several types of tests:

### Unit Tests
```bash
./gradlew test
```

### UI Tests
```bash
./gradlew uiTest
```

Note: The UI tests require a running application and browser. Configuration can be adjusted in `application-test.properties`.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

---

Created by [Devon Hillard](https://github.com/devondragon/) at [Digital Sanctuary](https://www.digitalsanctuary.com/)

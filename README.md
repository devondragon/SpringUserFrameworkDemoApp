# Spring User Framework Demo Application

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java Version](https://img.shields.io/badge/Java-17%2B-brightgreen)](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.3-green)](https://spring.io/projects/spring-boot)

A comprehensive demonstration application for the [Spring User Framework](https://github.com/devondragon/SpringUserFramework), showcasing how to implement user management features in a Spring Boot web application.

![Spring User Framework Demo Screenshot](/docs/images/Register.jpeg)

## Table of Contents
- [Overview](#overview)
- [Features](#features)
- [Quick Start](#quick-start)
- [Configuration](#configuration)
- [Project Structure](#project-structure)
- [Running the Application](#running-the-application)
- [Development Tools](#development-tools)
- [Notes](#notes)


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

1. **Run the application**


   - Then choose one of the following:
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


### Configuration Guide

#### **Database**
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

### Development Tools

#### Spring Boot DevTools
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

### Notes

- This demo is based on the principles outlined in the [Baeldung Spring Security Course](https://www.baeldung.com/learn-spring-security-course).
- Feel free to customize and extend the provided functionality to suit your needs.
**Disclaimer:** This is a demo project provided as-is with no guarantees of performance, security, or production readiness.


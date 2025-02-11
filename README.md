# SpringUserFramework Demo Application

This **Demo Application** demonstrates the capabilities of the [SpringUserFramework](https://github.com/devondragon/SpringUserFramework), a Java Spring Boot User Management Framework. The application showcases key features such as user registration, login, logout, forgot password flows, and Single Sign-On (SSO) integration with Google and Facebook.

The goal of this demo is to provide:
- A fully functional example of how to integrate the SpringUserFramework into your Spring Boot project.
- Example configurations for database, email, SSO, and other features.
- Working frontend pages, build with Bootstrap, for easy customization and extension.

---

## Features Demonstrated
- **User Management**:
  - Registration with optional email verification.
  - Login and logout functionality.
  - Forgot password workflow.
- **Security Features**:
  - CSRF protection (example AJAX implementation included).
  - Configurable account lockout after multiple failed login attempts.
- **Audit Logging**:
  - Framework-generated audit trails for login attempts, role assignments, and security events.
- **SSO Integration**:
  - OAuth2 login with Google and Facebook.
- **Customizable Role and Privilege System**:
  - Define roles, privileges, and inheritance through configuration.
- **Configuration Management**:
  - Example `application.yml` and profile-specific configurations for flexibility.
- **Docker Setup**:
  - Docker Compose for running the application with a database and mail server.

---

## Note:

### This project is a work in progress
I have been using it to test the new SpringUserFramework.  I have not tested all the steps in the README, nor started from scratch with this project.  I will be doing that soon.  If you have any issues, please let me know.  Please expect this README, and this project, to be improved in the future.


### Tests are currently not working
Tests are currently not working. This will be fixed in the future.  For now, just build with the following command:
```bash
./gradlew build -xtest
```

## Getting Started

### Prerequisites
- JDK 17 or later
- Gradle or Maven (if building locally)
- Docker (optional, for running the application with the provided `docker-compose.yml`)
- MariaDB or other supported database for user storage

---

### Quickstart

1. **Clone the Repository**:
   ```bash
   git clone https://github.com/devondragon/SpringUserFrameworkDemoApp.git
   cd SpringUserFrameworkDemoApp
   ```

2. **Setup Configuration**:
   - Copy the provided example configuration:
     ```bash
     cp src/main/resources/application-local.yml-example src/main/resources/application-local.yml
     ```
   - Update the file with your local database credentials, email server settings, and SSO keys.

3. **Run the Application**:
   - Using Gradle:
     ```bash
     ./gradlew bootRun
     ```
   - Or using Docker Compose:
     ```bash
     docker-compose up --build
     ```

4. **Access the Application**:
   - Navigate to `http://localhost:8080` in your browser.

---

### Configuration Guide

#### **Database**
The demo uses MariaDB as the default database. You can quickly spin up a MariaDB instance using Docker:
```bash
docker run -p 127.0.0.1:3307:3306 --name springuserframework \
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
    username: your-email@example.com
    password: your-email-password
    properties:
      mail.smtp.auth: true
      mail.smtp.starttls.enable: true
```

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

---

### Docker Support

#### Running Locally with Docker Compose
This repository includes a `docker-compose.yml` file to simplify local setup. The stack includes:
- Spring Boot Application
- MariaDB Database
- Postfix Mail Server (for testing email functionality)

To launch the stack:
```bash
docker-compose up --build
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
- **Disclaimer**: No warranty or guarantee of functionality, performance, or security is provided. Use at your own risk.


# Failing Tests Summary

## Test Statistics
- **Total Tests**: 303
- **Failed Tests**: 72
- **Skipped Tests**: 52
- **Passing Tests**: 179

## Test Files with Failures

### 1. Password Reset Tests (`PasswordResetApiTest.java`)
**Failed Tests**: 10
- Password reset initiation for valid email
- Token validation (invalid, expired, tampered)
- Missing/invalid email handling
- Multiple reset requests
- Concurrent request handling
- Token cleanup

### 2. Password Reset Completion Tests (`PasswordResetCompletionTest.java`)
**Failed Tests**: 7
- Password complexity enforcement
- Token reuse prevention
- Expired/invalid token handling
- Password mismatch handling
- Password reset with valid token
- Old password verification after reset

### 3. Authentication Tests (`AuthenticationIntegrationTest.java`)
**Failed Tests**: 3
- Access to protected resources when authenticated
- Redirect to login page for protected resources
- Redirect to saved request after login

### 4. API Security Tests (`ApiSecurityTest.java`)
**Failed Tests**: 20
- Authentication requirements (5 tests)
- Authorization/role-based access (3 tests)
- CSRF protection (6 tests)
- Rate limiting (1 test)
- Security headers (1 test)
- Session management (2 tests)

### 5. Authenticated User API Tests (`AuthenticatedUserApiTestSimplified.java`)
**Failed Tests**: 8
- Delete account operations (3 tests)
- Update password operations (4 tests)
- Update user profile (1 test)

### 6. Admin User Management Tests (`AdminUserManagementTest.java`)
**Failed Tests**: 6
- Admin operations (2 tests)
- Role hierarchy verification (2 tests)
- Role-based visibility (2 tests)

### 7. User Registration Tests (Multiple files)
**Failed Tests**: 15
- `UserRegistrationComprehensiveTest.java`: 10 tests
- `UserRegistrationCoreTest.java`: 2 tests
- `UserRegistrationEdgeCaseTest.java`: 3 tests

### 8. Security Configuration Tests (`SecurityConfigurationTest.java`)
**Failed Test**: 1
- Authenticated user access to protected endpoints

### 9. Password Reset API Simplified Tests (`PasswordResetApiTestSimplified.java`)
**Failed Tests**: 3
- Empty email handling
- Missing email handling
- Email format validation

## Test Files with Disabled Tests (@Disabled)

### 1. `UserApiTest.java`
- 4 tests disabled due to transaction isolation and Spring Security issues

### 2. `GoogleOAuth2IntegrationTest.java`
- Entire test class disabled (requires OAuth2 mock server)

### 3. `AuditLoggingIntegrationTest.java`
- Entire test class disabled (async timing issues)

### 4. `EmailVerificationEdgeCaseTest.java`
- Entire test class disabled (email verification timing issues)

### 5. `AccountLockoutIntegrationTest.java`
- 1 test disabled (requires time manipulation)

### 6. `DisabledTestExample.java`
- Example file showing different types of disabled tests

## Key Failure Patterns

1. **Authentication/Authorization**: Most failures relate to authentication setup, especially with DSUserDetails and Spring Security configuration
2. **CSRF Token Validation**: Many API tests fail due to CSRF token requirements
3. **Transaction Isolation**: Tests involving database operations often fail due to transaction boundaries
4. **Password Reset Flow**: Complete password reset workflow has multiple failure points
5. **Role-Based Access**: Admin and role hierarchy tests show authorization issues
6. **Session Management**: Session handling and security headers need attention

## Recommendations

1. Fix authentication setup for API tests (DSUserDetails configuration)
2. Properly handle CSRF tokens in test setup
3. Review transaction boundaries in integration tests
4. Fix password reset token generation and validation
5. Verify role hierarchy and authorization configuration
6. Address timing issues in async operations (audit logging, email verification)
# Changelog

All notable changes to the Spring User Framework Demo App will be documented in this file.

This project serves as a living reference implementation for the
[Spring User Framework](https://github.com/devondragon/SpringUserFramework). Rather than
formal releases, entries are grouped by date to track the project's evolution alongside
the library.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## 2026-08-15

### Added
- Admin lock/unlock API (`POST /admin/lockAccount`, `POST /admin/unlockAccount`), guarded by
  `ADMIN_PRIVILEGE` and backing the admin actions page, whose JavaScript already called those paths
- `keycloak/README.md`: contents, ports, credentials, and realm re-export for the Keycloak stack
- `.dockerignore`, so the image build context excludes `build/`, `.git/`, and local config

### Changed
- Rewrote `README.md` and split its content into `docs/CONFIGURATION.md`, `docs/DEVELOPMENT.md`,
  `docs/TESTING.md`, `docs/EXTENDING.md`, `docs/AUTHENTICATION.md`, and `keycloak/README.md`
- `Dockerfile` is now multi-stage on Java 21: a JDK stage runs `bootJar` inside the image, a JRE
  stage runs it, so `docker compose up --build` works from a fresh clone with no local Gradle build
- The Docker demo stack sets `USER_REGISTRATION_SENDVERIFICATIONEMAIL=false` in `compose.yaml`, so
  registered accounts are enabled immediately instead of waiting on mail the relay cannot deliver
- Moved `spring.docker.compose.file: compose.dev.yaml` from `application-local.yml-example` into base
  `application.yml`, so `./gradlew bootRun` starts its database without a copied config file
- `build.gradle` no longer extends `runtimeOnly` from `developmentOnly`, keeping
  `spring-boot-docker-compose` out of the packaged jar (it made containerized runs fail at startup)
- `application-local.yml-example` now points at the `compose.dev.yaml` database and seeds
  `data-local.sql`, so the `local` profile has sample events
- Added `/actuator/health` to `user.security.unprotectedURIs` for container healthchecks; the rest of
  `/actuator` still requires a login
- `src/main/resources/application-docker-keycloak.yml` is now tracked; it holds only environment
  variable placeholders, so there is nothing to copy before running the Keycloak stack
- `mise.toml` pins Java 21
- `@Disabled` test annotations now point at `docs/TESTING.md`

### Fixed
- Keycloak OIDC stack, which could not complete a login: the realm export is now named `demo` rather
  than `master` (Keycloak skips a `master` import), the client secret matches `keycloak.env`,
  authorization services that blocked the import were removed, a `demo` user with an email address
  was added, the browser-facing and container-facing Keycloak URLs are split, the client's redirect
  URI is narrowed to the exact callback, and the healthcheck probes the realm's discovery document
- `DemoSessionProfile` is now `@SessionScopedProfile`; as a plain `@Component` it was a singleton
  shared by every HTTP session
- `DomainRegistrationGuard`'s Javadoc link to the framework's registration guard documentation

### Removed
- Root `CONFIG.md` (a stale copy of the framework's property reference), `docs/HELP.md` (Spring
  Initializr boilerplate), `docs/TEST-ANALYSIS.md` (absorbed into `docs/TESTING.md`), and the
  `TempTest` startup debug logger

### Dependencies
- **Spring User Framework 5.3.0**, Spring Boot 4.1.0 (2026-08-14)
- Backfill of the bumps between the entries: framework 4.3.1 (2026-03-22, missing from that entry),
  4.4.0, 5.0.0, and 5.0.1 (2026-06-15), 5.1.0 (2026-07-10), 5.1.1 (2026-07-24), 5.2.0 (2026-08-12);
  Gradle wrapper 9.4.1 to 9.7.0 (2026-08-13). Spring Boot stayed on 4.0.4 until the 4.1.0 bump above

## 2026-03-22

### Changed
- Refactored Docker Compose file naming to avoid V2 precedence conflict (#68)
  - `compose.yaml` → `compose.dev.yaml` (dev dependencies for `bootRun`)
  - `docker-compose.yml` → `compose.yaml` (full deployable stack)
  - Added `spring.docker.compose.file` to `application-local.yml-example`
  - Updated README with compose file descriptions and Docker Compose V2 syntax
- Refactored `TestDataController` to use `Instant` instead of `Date` for registration dates, aligning with library changes (#65)

### Dependencies
- Spring Boot 4.0.4
- Gradle version bump

## 2026-03-12

### Added
- Sample `RegistrationGuard` demonstrating domain-restricted registration (#62)
- Made allowed domain configurable via properties, with unit tests

### Fixed
- Use `Locale.ROOT` for `toLowerCase()` in domain comparison for locale safety

### Dependencies
- **Spring User Framework 4.3.0**

## 2026-02-27

### Dependencies
- **Spring User Framework 4.2.1** (stable release)

## 2026-02-23

### Added
- Passwordless passkey-only account UI — demonstrates accounts that use only WebAuthn with no password (#53)
- Playwright tests for passwordless UI and auth method flows

### Fixed
- Delete `DemoUserProfile` before user in `TestDataController` to avoid FK violations
- PR review feedback for passwordless UI

## 2026-02-21

### Added
- WebAuthn/Passkey registration and login support for the demo app
- WebAuthn/Passkey documentation in README

### Changed
- Replaced passkey rename JS `prompt()` with Bootstrap modal dialog for better UX
- Hardened WebAuthn JavaScript with production-ready error handling and config
- Switched Claude GitHub Actions from OAuth token to API key auth

### Fixed
- Passkey label overflow and rename length validation

### Dependencies
- **Spring User Framework 4.2.0**
- Spring Boot 4.0.3
- MariaDB driver 12.2
- Gradle 9.3.1

## 2026-02-15

### Added
- Claude Code GitHub Actions workflow for automated PR review (#52)

### Fixed
- Use `pull_request_target` for Claude review to support fork PRs

## 2026-01-26

### Added
- Playwright E2E test framework for browser-based integration testing (#50)

## 2026-01-25

### Added
- Sample event data for local development, making it easier to demo event management out of the box (#49)

### Dependencies
- **Spring User Framework 4.0.2**

## 2026-01-07

### Fixed
- Use dynamic dates in `AdminRoleAccessControlTest` to prevent time-dependent failures

### Dependencies
- General dependency updates

## 2025-12-15

### Changed
- Updated Dependabot configuration for weekly dependency checks

## 2025-12-14

### Changed
- **Upgraded to Spring Boot 4.0.0 with Java 21** — major platform upgrade
- Updated test imports for Spring Boot 4 modular packages
- Added version compatibility table to README

### Fixed
- `unprotectedURIs` typo causing Spring Security 7 startup failure

### Dependencies
- **Spring User Framework 4.0.0**

## 2025-10-26

### Added
- Password validation fixes for the demo registration form
- Claude Code Review and PR Assistant GitHub Actions workflows
- Initial project structure as tracked in this repository

### Dependencies
- **Spring User Framework 3.5.1**
- Spring Boot 3.5.6

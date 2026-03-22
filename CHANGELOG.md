# Changelog

All notable changes to the Spring User Framework Demo App will be documented in this file.

This project serves as a living reference implementation for the
[Spring User Framework](https://github.com/devondragon/SpringUserFramework). Rather than
formal releases, entries are grouped by date to track the project's evolution alongside
the library.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## 2026-03-22

### Changed
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

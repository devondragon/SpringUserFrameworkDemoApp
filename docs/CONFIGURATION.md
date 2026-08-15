# Configuration

Base [`application.yml`](../src/main/resources/application.yml) holds the framework defaults this
demo runs with: mail transport, datasource, session/security settings, role/privilege map, and the
Docker Compose integration used by `bootRun`. Each profile file below overrides a subset of those
values for one scenario (local dev, production, tests, and so on). For the full property reference
(every key the framework recognizes, not just the ones this demo sets), see the framework's
[CONFIG.md](https://github.com/devondragon/SpringUserFramework/blob/main/CONFIG.md).

## Profiles

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

`local`, `dev`, `prd`, and `docker-keycloak` are base profiles you choose directly, one at a time, the
way the command above chooses `local`. `test` is not chosen by hand: `./gradlew test` applies it
automatically. `playwright-test` is meant to be combined with a base profile rather than run alone
(see its row below). `mfa` and `registration-guard` are opt-in add-ons with no base settings of their
own; combine one with a base profile by listing both, comma-separated, in `--spring.profiles.active`
(Spring Boot applies later profiles' properties over earlier ones when the same key is set in both).
If you omit `--args` entirely, `bootRun` still defaults to `local`: `build.gradle:118-123` sets
`SPRING_PROFILES_ACTIVE=local` unless you pass a Gradle project property, e.g.
`./gradlew bootRun -Pprofiles=local,mfa`.

| Profile | File | Purpose | What it overrides | Activate |
| --- | --- | --- | --- | --- |
| `local` | [`application-local.yml-example`](../src/main/resources/application-local.yml-example) → `application-local.yml` (gitignored, you create it) | Everyday local development | Debug logging, DevTools restart/LiveReload, seed-data loading, example OAuth2 client registrations, `sendVerificationEmail: false`, `allowInitialPasswordSetWithoutStepUp: true` | `--spring.profiles.active=local` |
| `dev` | [`application-dev.yml`](../src/main/resources/application-dev.yml) | Debug-heavy dev server; also what the Docker demo stack (`compose.yaml`) runs the app container as | Debug logging, insecure session cookie, `audit.flushOnWrite: true` | `--spring.profiles.active=dev` |
| `prd` | [`application-prd.yml`](../src/main/resources/application-prd.yml) | Production | Thymeleaf caching on, `ddl-auto: validate`, env-driven datasource, strict/secure cookies, `WARN` logging, limited actuator exposure, env-driven WebAuthn RP identity and `appUrl`, `requireCanonicalAppUrl: true`, no fallback for the remember-me key | `--spring.profiles.active=prd` |
| `test` | [`src/test/resources/application-test.properties`](../src/test/resources/application-test.properties) | Automated JUnit suite | Per-context isolated H2 database, MFA off, test-only `unprotectedURIs`. It also sets `maxFailedLoginAttempts`/`lockoutDurationMinutes` (lines 16-17), but those aren't the framework's property names (`failedLoginAttempts`/`accountLockoutDuration`), so they don't bind; lockout stays at the inherited default (10 attempts / 30 min) | Applied automatically by `./gradlew test` |
| `playwright-test` | [`application-playwright-test.yml`](../src/main/resources/application-playwright-test.yml) | Playwright E2E runs; enables the Test API (`TestDataController`, `TestApiSecurityConfig`, localhost-only) | Disables verification/reset emails, pins `appUrl` to `http://localhost:8080`, `allowInitialPasswordSetWithoutStepUp: true`, MFA off | Combine with a base profile, e.g. `local,playwright-test` (see [TESTING.md](TESTING.md)) |
| `docker-keycloak` | [`application-docker-keycloak.yml-example`](../src/main/resources/application-docker-keycloak.yml-example) → `application-docker-keycloak.yml` (gitignored) | OIDC login against the bundled Keycloak stack | Keycloak OAuth2 client/provider from `DS_SPRING_USER_KEYCLOAK_*` env vars, insecure session cookie, `audit.flushOnWrite: true` | `--spring.profiles.active=docker-keycloak`, normally set for you as `SPRING_PROFILES_ACTIVE` inside `docker-compose-keycloak.yml` |
| `mfa` | [`application-mfa.yml`](../src/main/resources/application-mfa.yml) | Add-on: require PASSWORD + WEBAUTHN | `user.mfa.enabled: true` (base `application.yml:126` has it `false`); once enabled, the framework auto-unprotects the configured MFA entry-point URIs at runtime, including the challenge page, so a partially-authenticated user can reach them; the profile's yml additionally adds the passkey enrollment endpoints `/webauthn/register/options` and `/webauthn/register` to `unprotectedURIs` (line 25) so that user can register their first passkey; `allowInitialPasswordSetWithoutStepUp: true` | Combine with a base profile, e.g. `local,mfa` |
| `registration-guard` | none (no yml; `@Profile("registration-guard")` on [`DomainRegistrationGuard`](../src/main/java/com/digitalsanctuary/spring/demo/registration/DomainRegistrationGuard.java)) | Add-on: domain-restricted registration demo | Activates a `RegistrationGuard` bean that restricts form/passwordless registration to one email domain (`registration.guard.allowed-domain`, default `@example.com`); OAuth2/OIDC registration is unaffected | Combine with a base profile, e.g. `local,registration-guard` |

See [AUTHENTICATION.md](AUTHENTICATION.md) for the mechanics behind `mfa`
([#mfa](AUTHENTICATION.md#mfa)), `docker-keycloak` ([#keycloak](AUTHENTICATION.md#keycloak)),
WebAuthn passkeys ([#passkeys](AUTHENTICATION.md#passkeys)), and `registration-guard`
([#registration-guard](AUTHENTICATION.md#registration-guard)).

## Getting started locally

1. Copy the example file and edit it: `cp src/main/resources/application-local.yml-example src/main/resources/application-local.yml`. It is gitignored, so your edits (and any real credentials) never get committed.
2. This step matters: base `application.yml` leaves `user.registration.sendVerificationEmail: true` (`application.yml:113`) and points `spring.mail.host` at an SES endpoint with no credentials (`application.yml:2-6`), so a fresh clone that skips step 1 starts fine but can never send the verification email a new registration needs, and you cannot log in. `application-local.yml-example` sets `sendVerificationEmail: false` (`application-local.yml-example:128`), so once you copy it, registered accounts are enabled immediately. To exercise the real verification flow instead, set it back to `true` and point `spring.mail.*` at a real SMTP server. The Docker demo stack (`compose.yaml`) disables verification email the same way; see [Mail](#mail).
3. At minimum, set `spring.mail.username`, `spring.mail.password`, and `spring.mail.host` if you want outbound mail to work locally. Set the `spring.security.oauth2.client.registration.*` client IDs/secrets only if you want to exercise social/Keycloak login.
4. Docker Compose integration: base `application.yml` sets `spring.docker.compose.file: compose.dev.yaml` (lines 66-73), so `./gradlew bootRun` under any profile starts a MariaDB 12.2 container (`springuser`/`springuser`, port 3306) automatically and stops it when the app stops. Set `spring.docker.compose.enabled: false` (commented hint right below the `file:` line) to point at a database you manage yourself instead.
5. Seed data: `application-local.yml-example` sets `spring.sql.init.mode: always` and `spring.sql.init.platform: local`, plus `spring.jpa.defer-datasource-initialization: true` (lines 25-30), so every boot under the `local` profile loads [`data-local.sql`](../src/main/resources/data-local.sql) (sample events). The script uses `INSERT IGNORE`, so re-running it on every start is safe.

## Environment variables

Required, no fallback:

| Variable | Meaning |
| --- | --- |
| `REMEMBER_ME_KEY` | Signs remember-me tokens. Base `application.yml` falls back to a random UUID per boot (`application.yml:157`) so the demo runs without it, but `application-prd.yml:57` has no fallback: `prd` fails to start unless this is set. |

Recognized elsewhere (fall back to a demo default when unset):

| Variable | Meaning |
| --- | --- |
| `APP_URL` | Canonical base URL for security email links in `prd` (`application-prd.yml:47`, default `https://example.com`). |
| `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD` | Production datasource (`application-prd.yml:10-12`). |
| `WEBAUTHN_RP_ID`, `WEBAUTHN_RP_NAME`, `WEBAUTHN_ALLOWED_ORIGINS` | WebAuthn relying-party identity in `prd` (`application-prd.yml:41-43`). |
| `DS_SPRING_USER_KEYCLOAK_CLIENT_ID`, `_CLIENT_SECRET`, `_PROVIDER_ISSUER_URI`, `_PROVIDER_AUTHORIZATION_URI`, `_PROVIDER_TOKEN_URI`, `_PROVIDER_USER_INFO_URI`, `_PROVIDER_JWK_SET_URI` | Keycloak OAuth2 client and provider endpoints for `docker-keycloak` (`application-docker-keycloak.yml-example:29-45`); supplied by [`keycloak.env`](../keycloak.env) when you run `docker-compose-keycloak.yml`. |
| `SELINUX_LABEL` | Suffix on the mailserver's bind-mounted config path in `compose.yaml`/`docker-compose-keycloak.yml`. Unset by default; Docker Compose prints a harmless warning about it. |

Any framework property can also be set through Spring's relaxed binding (`SCREAMING_SNAKE_CASE` of
the dotted key). The Docker demo stack (`compose.yaml`) does this for the app container:
`SPRING_DATASOURCE_URL`/`_USERNAME`/`_PASSWORD` (→ `spring.datasource.*`), `SPRING_PROFILES_ACTIVE`,
`SPRING_MAIL_HOST`/`_PORT` and the `SPRING_MAIL_PROPERTIES_MAIL_SMTP_*` keys (→ `spring.mail.*`), and
`USER_REGISTRATION_SENDVERIFICATIONEMAIL` (→ `user.registration.sendVerificationEmail`). The same
pattern works for any other key, e.g. `USER_SECURITY_BCRYPTSTRENGTH` for `user.security.bcryptStrength`.

## Mail

- `spring.mail.username`, `spring.mail.password`, `spring.mail.host`, `spring.mail.port` (`application.yml:2-6`) configure the SMTP transport used for verification, password-reset, and notification email. The base file's `host` is a placeholder SES endpoint; set real credentials in your profile.
- `user.registration.sendVerificationEmail` (`application.yml:113`) controls whether a new account must click a verification link before it can log in. `false` enables the account immediately at registration.
- The Docker demo stack's `mailserver` service (`compose.yaml`) is a relay only: `SMTP_ONLY: 1` (`compose.yaml:48`) with no route to real inboxes. That stack sets `USER_REGISTRATION_SENDVERIFICATIONEMAIL: "false"` (`compose.yaml:85`) so registered accounts activate immediately instead of waiting on mail nothing will deliver.
- `user.mail.fromAddress` sets the `From` address on outbound mail; it is set per profile (e.g. `application-local.yml-example:141`), not in the base file.

## Security settings this demo sets

- **Bcrypt strength**: `user.security.bcryptStrength: 12` (`application.yml:148`); `testHashTime: true` (`application.yml:149`) logs the measured hash time at startup so you can tune it.
- **Failed-login lockout**: `failedLoginAttempts: 10`, `accountLockoutDuration: 30` minutes (`application.yml:146-147`).
- **Session timeout**: `server.servlet.session.timeout: 30m`, with `secure` and `http-only` cookie flags (`application.yml:92-96`).
- **Default action / protected surface**: `defaultAction: deny` with an explicit `unprotectedURIs` allowlist, plus `protectedURIs` and `disableCSRFdURIs` (`application.yml:150-162`).
- **Remember-me**: `rememberMe.enabled: true`, signing key from `REMEMBER_ME_KEY` with a random-UUID fallback (`application.yml:151-158`).
- **Canonical app URL** (`user.security.appUrl`): prevents Host-header poisoning of password-reset/verification email links (SUF-01 / CWE-640); when unset, the framework derives the host from the (spoofable) request `Host` header and logs a startup warning. Base `application.yml:145` sets `http://localhost:8080`; `prd` drives it from `${APP_URL:https://example.com}` (`application-prd.yml:47`) and also sets `requireCanonicalAppUrl: true` (`application-prd.yml:49`), so `prd` fails to start without it; `playwright-test` pins it explicitly to `http://localhost:8080` (`application-playwright-test.yml:32`).
- **Allow initial password set without step-up** (`user.security.allowInitialPasswordSetWithoutStepUp`): controls `POST /user/setPassword`, which lets a passkey-only account set an initial password. As of the framework's SUF-02 hardening, this endpoint returns `403` unless a `StepUpService` bean exists or this is `true`. This demo has no `StepUpService`, so it sets the flag `true` in `local` (`application-local.yml-example:138`), `mfa` (`application-mfa.yml:24`), and `playwright-test` (`application-playwright-test.yml:35`) to keep the passkey flow usable, and leaves it at its secure default `false` in `prd` (`application-prd.yml:50`).

For OAuth2/OIDC, WebAuthn passkeys, MFA, and the registration guard, see
[AUTHENTICATION.md](AUTHENTICATION.md).

## Roles and monitoring

`user.roles.roles-and-privileges` and `user.roles.role-hierarchy` (`application.yml:200-222`) define
this demo's `ROLE_ADMIN` > `ROLE_MANAGER` > `ROLE_USER` hierarchy and the privileges behind each of
the demo's event-management and user-management actions; edit them in place if you add roles or
privileges. `management.newrelic.metrics.export.api-key` / `.account-id` (`application.yml:83-87`)
are unset placeholders: leave them blank to skip New Relic, or fill them in per profile to export
metrics.

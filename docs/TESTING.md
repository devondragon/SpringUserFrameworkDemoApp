# Testing

How this demo app is tested: JUnit tests, Playwright E2E tests, and the test-only API that
supports Playwright. For the framework's own testing guide, see
[SpringUserFramework/docs/TESTING.md](https://github.com/devondragon/SpringUserFramework/blob/main/docs/TESTING.md).

## JUnit tests

```bash
./gradlew test                              # all tests
./gradlew test --tests UserApiTest          # one class
./gradlew test --tests UserApiTest.resetPassword  # one method
```

Tests run under the `test` Spring profile
([`application-test.properties`](../src/test/resources/application-test.properties)), backed by
an in-memory H2 database with a per-context unique name
(`jdbc:h2:mem:testdb-${random.uuid}`) so each Spring context is isolated.

Two more profiles support OAuth2 tests: `oauth2-mock`
([`application-oauth2-mock.properties`](../src/test/resources/application-oauth2-mock.properties)),
used by
[`GoogleOAuth2IntegrationTest`](../src/test/java/com/digitalsanctuary/spring/user/oauth2/GoogleOAuth2IntegrationTest.java)
(currently `@Disabled`); and `oauth2test`
([`application-oauth2test.properties`](../src/test/resources/application-oauth2test.properties)),
documented in
[`oauth2/README.md`](../src/test/java/com/digitalsanctuary/spring/user/oauth2/README.md) for
tests written against `OAuth2TestConfiguration` but not currently used by an active test.

## Test layout

- `src/test/java/com/digitalsanctuary/spring/user/...`: tests for the framework's user
  management surface (`api/`, `concurrent/`, `config/`, `integration/`, `json/`, `oauth2/`,
  `security/`).
- `src/test/java/com/digitalsanctuary/spring/demo/...`: tests for the demo app's own code
  (`controller/`, `event/`, `mfa/`, `registration/`, `user/profile/session/`, `DemoTests.java`).

[`IntegrationTest`](../src/test/java/com/digitalsanctuary/spring/user/test/annotations/IntegrationTest.java)
composes `@SpringBootTest` (against `UserDemoApplication`), `@AutoConfigureMockMvc`,
`@AutoConfigureDataJpa`, `@ActiveProfiles("test")`, and `@Transactional` (rollback per test).

Test data builders live in
[`.../user/test/builders/`](../src/test/java/com/digitalsanctuary/spring/user/test/builders/):
`UserTestDataBuilder`, `RoleTestDataBuilder`, `TokenTestDataBuilder`.

## Disabled tests

```bash
/usr/bin/find src/test -name '*.java' | wc -l                       # test files
/usr/bin/find src/test -name '*.java' | xargs grep -l @Disabled | wc -l  # files with @Disabled
```

As of this writing that's 64 test files, 17 with `@Disabled`. 15 were disabled during a REST API
alignment pass and point back to this file; they fall into these categories:

- **Auth expectations**: test expects a specific JSON error body on auth failure; Spring
  Security returns an empty 401/403, or `DSUserDetails` isn't mocked the way the test assumes.
- **OAuth2/OIDC**: needs mock provider infrastructure not wired up for that test.
- **Response format**: test assumes form-encoded or HTML where the endpoint returns JSON, or the
  reverse.
- **Audit logging**: asserts on log output with timing assumptions that don't hold under async
  logging in the test environment.
- **Email/token verification**: assumes mock email service or token timing not configured for
  that test.
- **Transaction isolation**: a user created in test setup isn't visible to the REST endpoint
  within the same transaction.

The categories are representative, not exhaustive: `AdminUserManagementTest` (role hierarchy and
admin operations configuration) and one case in `SecurityConfigurationTest` (`/protected.html`
returns 404) fit none of them.

They're kept, not deleted: each documents an expected behavior or a gap worth revisiting as a
framework improvement. The other two (`DisabledTestExample.java`,
`AccountLockoutIntegrationTest.java`) are disabled for unrelated, self-contained reasons
documented inline.

## Playwright E2E tests

Tests live in [`playwright/`](../playwright) (`@playwright/test`). To drive them through npm, install
once:

```bash
cd playwright && npm ci && npx playwright install
```

Then run the npm scripts from `playwright/` (`playwright/package.json`: `test`, `test:chromium`,
`test:headed`, `test:ui`). The Gradle wrapper tasks in [`build.gradle`](../build.gradle)
(`verification` group) are the other route, and they run from the repository root:
`./gradlew playwrightTest` / `playwrightTestChromium`. Both depend on `playwrightBrowsers` and
`playwrightInstall`, so the Gradle route installs the npm dependencies and the browsers itself and
needs no separate install step.

[`playwright.config.ts`](../playwright/playwright.config.ts) starts the app itself via
`webServer`: `./gradlew bootRun --args="--spring.profiles.active=${APP_PROFILES:-local,playwright-test}"`
against `http://localhost:8080`, reusing an already-running server unless `CI` is set. The
`playwright-test` profile
([`application-playwright-test.yml`](../src/main/resources/application-playwright-test.yml))
disables verification/reset emails (tests fetch tokens via the Test API instead), pins
`user.security.appUrl`, and sets `allowInitialPasswordSetWithoutStepUp: true` so the passkey-only
"set initial password" flow works without a `StepUpService` bean.

The `chromium`, `firefox`, `webkit`, `Mobile Chrome`, and `Mobile Safari` projects skip specs
tagged `@mfa-enabled` (`grepInvert`); a separate `chromium-mfa` project runs only those specs,
against a server started with the `mfa` profile added:

```bash
APP_PROFILES=local,playwright-test,mfa npx playwright test --project=chromium-mfa
```

**Test API**:
[`TestDataController`](../src/main/java/com/digitalsanctuary/spring/demo/test/api/TestDataController.java)
exposes `/api/test/*` (create/enable/unlock/delete a user, fetch verification and password-reset
tokens, health check), loaded only under `@Profile("playwright-test")`.
[`TestApiSecurityConfig`](../src/main/java/com/digitalsanctuary/spring/demo/test/config/TestApiSecurityConfig.java)
disables CSRF for `/api/test/**` and restricts it to requests from `127.0.0.1`,
`0:0:0:0:0:0:0:1`, or `localhost`; everything else is denied.

## CI

[`.github/workflows/tests.yml`](../.github/workflows/tests.yml) runs on pull requests and pushes
to `main`: **`unit-tests`** runs `./gradlew test` on Java 21. **`playwright-tests`** builds the
app, starts a `mariadb:12.2` service container, installs Playwright, then runs E2E twice: once
with `APP_PROFILES=playwright-test` against `chromium` (MFA off), once with
`APP_PROFILES=playwright-test,mfa` against `chromium-mfa` (MFA on).

# Development

## Prerequisites

- JDK 21. [`mise.toml`](../mise.toml) pins `java = "21"` if you use [mise](https://mise.jdx.dev/).
- Docker. `./gradlew bootRun` starts a MariaDB container for you (see below); Docker must be running.
- Node.js, only if you run the Playwright E2E suite; see [TESTING.md](TESTING.md).

## Running the app

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

This is the Spring Boot Gradle plugin's `bootRun` task. Add `--debug-jvm` to attach a debugger on
port 5005. Because the `spring.docker.compose.file: compose.dev.yaml` setting lives in base
`application.yml` (`application.yml:66-73`), `bootRun` always starts a MariaDB 12.2 container
(`springuser`/`springuser`, port 3306) automatically, whichever profile you pass, and stops it when
you stop the app. See [CONFIGURATION.md](CONFIGURATION.md) for what to edit first
(`application-local.yml`) and how to opt out of the auto-started database.

[`scripts/run.sh`](../scripts/run.sh) is a different path: it runs `./gradlew bootJar`, then
`java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:6332 -jar
build/libs/ds-spring-user-framework-demo-1.0.1-SNAPSHOT.jar --spring.profiles.active=local` (JDWP
debug agent on port 6332). Running from the packaged jar means `spring-boot-docker-compose` is not
on the classpath (it is `developmentOnly`), so the `spring.docker.compose.file` setting has no effect
here and no database gets started for you. Start one first, either
`docker compose -f compose.dev.yaml up -d` or your own MariaDB on `localhost:3306`, then run
`./scripts/run.sh`.

Spring Boot DevTools (`runtimeOnly` dependency) restarts the app automatically when a class changes,
but only under `bootRun`. It disables itself when the app is launched as a fully packaged jar via
`java -jar`, which is exactly what `scripts/run.sh` does, so that path has no auto-restart.

### LiveReload

The LiveReload `<script>` tag in
[`templates/layout.html`](../src/main/resources/templates/layout.html) is commented out
(`layout.html:65-66`):

```html
<!--<script th:if="${devOrLocalProfile}" th:src="'https://localhost:' + ${liveReloadPort} + '/livereload.js'"></script>-->
```

Uncomment it to enable browser auto-refresh on template/static changes, but note the URL is hardcoded
to `https://`, not conditional on scheme. The port comes from the framework's
[`LiveReloadGlobalControllerAdvice`](https://github.com/devondragon/SpringUserFramework/blob/main/src/main/java/com/digitalsanctuary/spring/user/util/LiveReloadGlobalControllerAdvice.java#L23-L37):
35739 when `spring.devtools.livereload.https=true`, 35729 otherwise. The real DevTools LiveReload
server always listens on plain HTTP at its default port, 35729, regardless of that flag, so:

- `spring.devtools.livereload.https=true`, already set by
  `application-local.yml-example:107` (so copying that example file puts you on this path
  immediately; the `docker-keycloak` profile does not set this property): the script requests
  `https://localhost:35739/livereload.js`. Nothing listens there by default; run
  `mitmproxy --mode reverse:http://localhost:35729 -p 35739` to terminate TLS on 35739 and forward to
  the real server on 35729.
- `spring.devtools.livereload.https=false` (the property's own default): the script requests
  `https://localhost:35729/livereload.js`, HTTPS against a server that only speaks plain HTTP, which
  does not connect. Uncommenting the tag with this setting does not work without also changing the
  template or running a proxy in front of 35729.

`.vscode/tasks.json` has "Start ngrok" and "Start mitmproxy" tasks (composed as "Start Dev Tools")
that automate the tunnel + proxy pair for the first case. See:

- [Spring Boot Live Reload](https://www.digitalsanctuary.com/java/springboot-devtools-auto-restart-and-live-reload.html)
- [HTTPS Live Reload Setup](https://www.digitalsanctuary.com/java/how-to-get-springboot-livereload-working-over-https.html)

## Docker Compose files

- [`compose.dev.yaml`](../compose.dev.yaml): database only. Started automatically by `bootRun`'s
  Docker Compose integration; not meant to be run directly, though `docker compose -f compose.dev.yaml
  up -d` works if you want the database without the app.
- [`compose.yaml`](../compose.yaml): the full demo stack: app + MariaDB + a relay-only mail
  container. `docker compose up -d` builds the app image (multi-stage [`Dockerfile`](../Dockerfile):
  a JDK-21 build stage runs `./gradlew --no-daemon bootJar -x test` (`Dockerfile:14`), a JRE-21 stage runs the resulting jar as a non-root
  user) and runs all three. The app container's healthcheck polls `GET /actuator/health`
  (`compose.yaml:86-90`), the only actuator endpoint left unauthenticated.
- [`docker-compose-keycloak.yml`](../docker-compose-keycloak.yml): the same app + MariaDB + mail
  setup plus a Keycloak container, for testing OIDC login. Start with
  `docker compose -f docker-compose-keycloak.yml up -d --build --wait`. See
  [`keycloak/README.md`](../keycloak/README.md) for ports, credentials, and the login walkthrough, and
  [AUTHENTICATION.md#keycloak](AUTHENTICATION.md#keycloak) for the OIDC mechanics.

## Gradle tasks

- `./gradlew test`: run the JUnit suite (`test` profile, H2 in-memory).
- `./gradlew bootJar`: build the executable jar.
- `./gradlew build -x test`: full build, skipping tests.
- `./gradlew dependencyUpdates`: report outdated dependencies (`com.github.ben-manes.versions` plugin).
- `./gradlew playwrightInstall` / `playwrightBrowsers` / `playwrightTest` / `playwrightTestChromium` /
  `playwrightReport`: Playwright E2E tasks, defined in `build.gradle`; see [TESTING.md](TESTING.md)
  for what each does and how they're wired together.

Run `./gradlew tasks --group verification` to list the verification-group tasks (including the
Playwright ones above) straight from the build.

## Logs

The app writes to `/opt/app/logs/user-app.log` (`application.yml:97-99`; `application-prd.yml:22-28`
sets `WARN`-level logging for the same file in `prd`). Security/user-lifecycle events go to a
separate audit log at `/opt/app/logs/user-audit.log` (`application.yml:135-138`,
`user.audit.logFilePath`).

## API surface

Swagger UI is at `/swagger-ui.html` (`springdoc.swagger-ui.path`, `application.yml:101-107`), scanning
`com.digitalsanctuary.spring.demo` and `com.digitalsanctuary.spring.user`. It documents this app's own
endpoints (e.g. [`EventAPIController`](../src/main/java/com/digitalsanctuary/spring/demo/event/EventAPIController.java)).
For the framework's `/user/*` endpoints (registration, login, password reset, profile update), see
the framework's [User Management docs](https://github.com/devondragon/SpringUserFramework#user-management).

## IDE setup

Enable annotation processing (Lombok is a `compileOnly` + `annotationProcessor` dependency; without
it the project won't compile in the IDE). `.vscode/tasks.json` has the ngrok/mitmproxy tasks used for
HTTPS LiveReload above.

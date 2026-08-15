# Keycloak stack

`docker-compose-keycloak.yml` runs the demo app against a Keycloak OIDC provider alongside the
built-in form login. Four containers: the app, Keycloak 25.0.6, one MariaDB shared by both, and a
mail server. The image is built from source inside Docker, so a fresh clone needs no local Gradle
build. `up` returns when Keycloak is answering and the app is healthy, about a minute cold.

```bash
docker compose -f docker-compose-keycloak.yml up -d --build
docker compose -f docker-compose-keycloak.yml down -v   # stop and delete the data
```

## Ports and credentials

| What | URL | Login |
| --- | --- | --- |
| Demo app | http://localhost:8080 | see below |
| Keycloak | http://localhost:8180 | `admin` / `admin` (master realm) |
| Keycloak HTTPS | https://localhost:8143 | self-signed, see `ssl/README.MD` |
| Keycloak management | port 9001 (container 9000) | HTTPS, serves `/health/*` and `/metrics` |
| MariaDB | localhost:3307 | `springuser` / `springuser` |

All of these are dev-only credentials committed to the repository. Do not reuse them.

## Log in through Keycloak

1. Open http://localhost:8080/user/login.html
2. Click "Login with Keycloak". The browser goes to Keycloak at http://localhost:8180.
3. Sign in as `demo` / `demo`.
4. You land back on http://localhost:8080/index.html?messageKey=message.login.success, signed in as
   Demo User. The app creates a local account for the Keycloak identity on first login
   (provider `KEYCLOAK`, email `demo@example.com`).

`admin` / `admin` is a master realm account for the admin console only, not a demo realm user.

## The realm

`realm/realm-export.json` is imported by `--import-realm` on first start. It defines the realm `demo`,
the confidential client `ds-spring-user-framework-demo`, and the `demo` user. The realm must not be
`master`: Keycloak creates the master realm before the import runs and the import strategy is
IGNORE_EXISTING, so a `master` export is skipped without an error.

To change the realm, edit it in the admin console, then export it back over the file. The management
port has to be moved because the running server already holds 9000:

```bash
docker exec keycloak.openid-provider /opt/keycloak/bin/kc.sh export \
  --realm demo --file /tmp/realm-export.json --http-management-port 9002
docker cp keycloak.openid-provider:/tmp/realm-export.json keycloak/realm/realm-export.json
```

This includes users and the real client secret. The admin console's own partial export writes the
secret as `**********` instead, which then no longer matches
`DS_SPRING_USER_KEYCLOAK_CLIENT_SECRET` in `keycloak.env` and breaks the login.

## Two hostnames

Keycloak is one server on two addresses: `localhost:8180` for the host browser, `keycloak:8080` for
the app container. `keycloak.env` splits the OIDC endpoints along that line, and
`src/main/resources/application-docker-keycloak.yml` explains why there is no `issuer-uri`.

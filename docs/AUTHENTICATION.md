# Authentication

Every authentication path this demo can exercise, how to run it, and where its code and configuration
live. Per-profile overrides are in [CONFIGURATION.md](CONFIGURATION.md), the framework's full property
list in [CONFIG.md](https://github.com/devondragon/SpringUserFramework/blob/main/CONFIG.md).
All pages below are served by the framework's `UserPageController` unless noted, with paths from
`user.security.*URI` in [`application.yml`](../src/main/resources/application.yml) (lines 165-177), and
call `/user/*` endpoints in its `UserAPI`. The demo supplies the HTML and JavaScript,
[`templates/user/`](../src/main/resources/templates/user) and
[`static/js/user/`](../src/main/resources/static/js/user), one module per page (page-to-endpoint map:
[EXTENDING.md](EXTENDING.md#reference-templates-javascript-and-messages)). Logs go to
`/opt/app/logs/user-app.log` and `/opt/app/logs/user-audit.log` (`application.yml:99`, `:136`), and
failed logins and lockouts land in the audit log.

## Username and password with email verification

1. Register at `/user/register.html` ([`register.html`](../src/main/resources/templates/user/register.html),
   [`register.js`](../src/main/resources/static/js/user/register.js)), which posts JSON to
   `POST /user/registration`, not form data.
2. What happens next depends on `user.registration.sendVerificationEmail`:
   - `true` (base `application.yml:113`): the account is created disabled, a verification email is sent,
     and the browser lands on `/user/registration-pending-verification.html`. The emailed link is
     `GET /user/registrationConfirm?token=...`, which enables the account. Lost it? Request another at
     `/user/request-new-verification-email.html`
     ([`resend-verification.js`](../src/main/resources/static/js/user/resend-verification.js) posts
     `POST /user/resendRegistrationToken`).
   - `false`: the account is created enabled, the framework logs the user straight in, and the browser
     lands on `/user/registration-complete.html`. `application-local.yml-example:128` sets it false and
     the Docker demo stack sets `USER_REGISTRATION_SENDVERIFICATIONEMAIL: "false"` (`compose.yaml:85`),
     so neither documented run path needs a working SMTP server.
3. Log in at `/user/login.html` ([`login.js`](../src/main/resources/static/js/user/login.js)). The form
   posts to `/user/login`; success redirects to `/index.html?messageKey=message.login.success`. Ten
   failed attempts lock the account for 30 minutes (`application.yml:146-147`).
4. Forgot password: `/user/forgot-password.html` posts `POST /user/resetPassword`, which emails a link
   to `GET /user/changePassword?token=...`. That endpoint validates the token and redirects to
   `/user/forgot-password-change.html`, which posts `POST /user/savePassword`
   ([`forgot-password.js`](../src/main/resources/static/js/user/forgot-password.js),
   [`reset-password.js`](../src/main/resources/static/js/user/reset-password.js)). Both reset steps need
   real mail, unlike registration. To change a password you know, `/user/update-password.html` posts
   `POST /user/updatePassword`.

## Passkeys

WebAuthn is on by default here. Four keys configure it, `application.yml:117-120`:
`user.webauthn.enabled: true`; `rpId: localhost`, the relying party ID, which must equal the browser's
hostname; `rpName: Spring User Framework Demo`, shown in the browser's passkey prompt; and
`allowedOrigins: http://localhost:8080`, which must equal the browser origin exactly, port included.
Sign-in additionally needs `/webauthn/authenticate/**` and `/login/webauthn` in
`user.security.unprotectedURIs` (`application.yml:160`), since both are called before a session exists;
enrollment and credential management sit behind authentication and need no entry.

In the UI: log in with a password, open `/user/update-user.html`, name the passkey and click "Add
Passkey" ([`webauthn-register.js`](../src/main/resources/static/js/user/webauthn-register.js)); sign in
with it from the "Sign in with Passkey" button on `/user/login.html`, shown only when the browser reports
WebAuthn support; rename and delete from that same panel, labels capped at 64 characters
([`webauthn-manage.js`](../src/main/resources/static/js/user/webauthn-manage.js)).
HTTP works on `localhost`; any other host needs HTTPS, so run `ngrok http 8080` and set `rpId` to the
tunnel hostname and `allowedOrigins` to the full `https://` origin (`application-prd.yml:41-43` drives all
three from `WEBAUTHN_RP_ID`, `WEBAUTHN_RP_NAME` and `WEBAUTHN_ALLOWED_ORIGINS`). The `user_entities` and
`user_credentials` tables are created by Hibernate (`ddl-auto: update`, `application.yml:58`), so there is
no migration step. Framework reference:
[WebAuthn / Passkeys](https://github.com/devondragon/SpringUserFramework/blob/main/README.md#webauthn--passkeys).

## Passwordless registration and setting a first password

`/user/register.html` shows a "Passwordless (Passkey)" toggle when the browser supports WebAuthn
([`register.js:31-64`](../src/main/resources/static/js/user/register.js)). In that mode the password
fields are hidden and the form posts `{firstName, lastName, email}` to
`POST /user/registration/passwordless` instead; that path is in `unprotectedURIs`
(`application.yml:160`), without which it would be unreachable under `defaultAction: deny`. The account
is created with no password, and the user then enrolls a passkey from `/user/update-user.html` as above.
A passkey-only account can add a password later. `/user/update-password.html` asks
`GET /user/auth-methods` on load ([`auth-methods.js`](../src/main/resources/static/js/user/auth-methods.js));
when `hasPassword` is false it drops the current-password field and posts `POST /user/setPassword`
instead of `/user/updatePassword`
([`update-password.js:19-42,68-99`](../src/main/resources/static/js/user/update-password.js)).
That endpoint is guarded (framework SUF-02): with no current password to verify it requires a
`StepUpService` bean, and returns `403` when none exists unless
`user.security.allowInitialPasswordSetWithoutStepUp` is `true`. This demo has no `StepUpService`, so it
sets the flag true where the flow has to be demonstrable (`application-local.yml-example:138`,
`application-mfa.yml:24`, `application-playwright-test.yml:35`) and leaves it at the secure default
`false` in `prd` (`application-prd.yml:50-52`).

## MFA

The `mfa` profile turns on `user.mfa.enabled` (`application-mfa.yml:20`), `false` in the base config
(`application.yml:126`). The factor list is `PASSWORD` then `WEBAUTHN` (`application.yml:127-129`), so a
password login alone reaches no protected page: a request for one is bounced to
`user.mfa.webauthnEntryPointUri`, `/user/mfa/webauthn-challenge.html` (`application.yml:133`). Walk it
through in this order:

1. Run with `local` only and enroll a passkey (see [Passkeys](#passkeys)). A user with no passkey
   cannot satisfy the WEBAUTHN factor and is locked out of every protected page, which is why the base
   config keeps MFA off (`application.yml:123-125`).
2. Restart with both profiles: `./gradlew bootRun --args='--spring.profiles.active=local,mfa'`.
3. Log in with your password at `/user/login.html`. Login itself still lands on the configured success
   page, `/index.html?messageKey=message.login.success` (`application.yml:167`), which is unprotected;
   the challenge redirect fires from the access-denied handler. So open any protected page, for example
   `/user/update-user.html`, and you are redirected to `/user/mfa/webauthn-challenge.html`.
4. Click "Verify with Passkey"
   ([`mfa-webauthn-challenge.js`](../src/main/resources/static/js/user/mfa-webauthn-challenge.js)) and
   the session becomes fully authenticated.

Two unprotection details, both explained in the comments at `application-mfa.yml:10-16`. The framework
unprotects the configured factor entry-point URIs at runtime, challenge page included, so the
partial-auth redirect cannot loop back onto itself. Separately, the profile adds
`/webauthn/register/options` and `/webauthn/register` to `unprotectedURIs` (`application-mfa.yml:25`) so
a partially-authenticated user can still enroll a first passkey (Spring Security still requires an
authenticated principal to store one, so this only relaxes the all-factors requirement). That relaxation
is endpoint-level only: the enrollment panel lives on the protected `/user/update-user.html`, so there is
no UI path to a first passkey once MFA is on. Hence step 1 above, and hence the E2E test calling
`registerPasskey` from a page script
([`mfa-flow.spec.ts:52-55`](../playwright/tests/mfa/mfa-flow.spec.ts)). The demo owns the challenge page,
[`templates/user/mfa/webauthn-challenge.html`](../src/main/resources/templates/user/mfa/webauthn-challenge.html),
plus its mapping in
[`PageController:59-62`](../src/main/java/com/digitalsanctuary/spring/demo/controller/PageController.java).

## OAuth2 with Google and Facebook

`user.registration.googleEnabled` and `user.registration.facebookEnabled` decide whether the buttons render
on `/user/login.html` and `/user/register.html`; both are `false` in `application.yml:114-115` and `true` in
`application-local.yml-example:129-130`. They link to `/oauth2/authorization/google` and
`/oauth2/authorization/facebook`, already covered by `/oauth2/authorization/*` in `unprotectedURIs`
(`application.yml:160`). Client IDs and secrets belong in `application-local.yml` (gitignored), never in
`application.yml`; copy the filled-in shape at
[`application-local.yml-example:35-57`](../src/main/resources/application-local.yml-example), and see the
framework's [SSO OAuth2 with Google and
Facebook](https://github.com/devondragon/SpringUserFramework/blob/main/README.md#sso-oauth2-with-google-and-facebook)
for the YAML block. A localhost callback is not ruled out: Google exempts `http://localhost` redirect URIs
from its HTTPS requirement, so `http://localhost:8080/login/oauth2/code/google` works once registered in
the provider console and set as `redirect-uri`. Facebook enforces HTTPS on redirect URIs by default for
apps created since March 2018, so a plain-HTTP localhost callback there depends on the app's settings,
normally while it is in development mode. Reach for `ngrok http 8080` when you need a public HTTPS callback
or want to test from another device; then also set `user.security.appUrl` to the tunnel URL
(`application-local.yml-example:132-135`), or emailed links still point at localhost.

## Keycloak

Start the stack, which runs the app against a bundled Keycloak 25.0.6 alongside the normal form login:

```bash
docker compose -f docker-compose-keycloak.yml up -d --build --wait
```

`--wait` holds until every container is healthy; plain `up -d` returns mid-boot. The first build resolves
dependencies and runs `bootJar` inside the image, several minutes; later starts are under a minute. The
realm, its client and the `demo` user are imported on first start from
[`keycloak/realm/realm-export.json`](../keycloak/realm/realm-export.json); stack contents and re-export
instructions are in [`keycloak/README.md`](../keycloak/README.md). The essentials:

| What | Address | Login |
| --- | --- | --- |
| Demo app | http://localhost:8080 | `demo` / `demo`, through Keycloak |
| Keycloak admin console | http://localhost:8180 | `admin` / `admin`, master realm, console only |
| Keycloak HTTPS | https://localhost:8143 | self-signed certificate |
| Keycloak management | port 9001 (container 9000) | `/health/*` and `/metrics`, HTTPS |
| MariaDB | localhost:3307 | `springuser` / `springuser` |

Open http://localhost:8080/user/login.html, click "Login with Keycloak", sign in as `demo` / `demo`.
First login creates a local account with provider `KEYCLOAK` and email `demo@example.com`. The realm is
`demo`, not `master`, so every OIDC URL is `/realms/demo/...`; `admin` / `admin` is a master realm
account and cannot sign in to the demo app.
The compose file sets `SPRING_PROFILES_ACTIVE: docker-keycloak`, and every value in
[`application-docker-keycloak.yml`](../src/main/resources/application-docker-keycloak.yml) comes from an
environment variable in [`keycloak.env`](../keycloak.env): `DS_SPRING_USER_KEYCLOAK_CLIENT_ID`,
`..._CLIENT_SECRET`, and `..._PROVIDER_AUTHORIZATION_URI` / `_TOKEN_URI` / `_USER_INFO_URI` /
`_JWK_SET_URI`. The authorization URI points at `http://localhost:8180` because only the browser calls it;
the other three point at `http://keycloak:8080`, over the compose network. The button is shown by
`user.registration.keycloakEnabled: true` (`application-docker-keycloak.yml:64`).
One limitation: `issuer-uri` is deliberately unset. Keycloak stamps the ID token `iss`
with its frontend URL (`http://localhost:8180/realms/demo`), which the app container cannot reach, and
Spring Boot treats `issuer-uri` as a discovery location it fetches at startup. No single URL works from
both a host browser and a container without editing `/etc/hosts`, so the property is omitted and Spring
Security skips only the `iss` comparison; signature (via `jwk-set-uri`), audience, nonce and expiry are
still validated. With real DNS in front of Keycloak, set `issuer-uri` and drop the two-hostname split.
Same reasoning in the file's comment, `application-docker-keycloak.yml:36-46`.

## Registration guard

The framework's `RegistrationGuard` SPI lets an application allow or deny each registration attempt (invite
codes, allowlists, domain restrictions). Every guard bean is consulted for form, passwordless and
OAuth2/OIDC sign-ups, and one denial rejects the registration with that guard's message. Full SPI contract,
including how to write your own:
[docs/REGISTRATION-GUARD.md](https://github.com/devondragon/SpringUserFramework/blob/main/docs/REGISTRATION-GUARD.md).
This demo ships
[`DomainRegistrationGuard`](../src/main/java/com/digitalsanctuary/spring/demo/registration/DomainRegistrationGuard.java),
which restricts form and passwordless registration to one email domain while allowing all OAuth2/OIDC
registration, annotated `@Profile("registration-guard")` so the default demo is unaffected.

```bash
# Only @example.com addresses can register via the form; OAuth2/OIDC still allowed
./gradlew bootRun --args='--spring.profiles.active=local,registration-guard'

# Override the allowed domain. It goes inside --args, as a Spring Boot argument, so that it reaches the
# forked application; a -D after the task name sets it on the Gradle JVM only and is not forwarded.
./gradlew bootRun \
  --args='--spring.profiles.active=local,registration-guard --registration.guard.allowed-domain=@mycompany.com'
```

| Setting | Default | Purpose |
| --- | --- | --- |
| `registration-guard` profile | off | Activates the sample guard bean |
| `registration.guard.allowed-domain` | `@example.com` | Domain form/passwordless registrations must match |

A non-matching address is denied with `Registration is restricted to <domain> email addresses.`

## Remember-me

`user.security.rememberMe.enabled: true` (`application.yml:151-152`) makes Spring Security issue a
`remember-me` cookie when the login form posts the checkbox on
[`login.html:78`](../src/main/resources/templates/user/login.html).
The signing key is `${REMEMBER_ME_KEY:${random.uuid}}` (`application.yml:157`). The random fallback means
the demo never runs on a publicly known key, at the cost of invalidating every remember-me cookie on
restart; set `REMEMBER_ME_KEY` to a long random value to keep tokens valid across restarts and instances.
The `prd` profile has no fallback (`application-prd.yml:57`), so startup fails unless the variable is set,
and it forces `useSecureCookie: true` (`application-prd.yml:61`) because Spring's default derives that
from `request.isSecure()`, false behind a TLS-terminating proxy. Two commented options sit next to the key
(`application.yml:158-159`): `tokenValiditySeconds` (default 14 days) and `usePersistentTokens`, which
stores tokens in the `persistent_logins` table so they can be revoked server-side.
[`playwright/tests/auth/remember-me.spec.ts`](../playwright/tests/auth/remember-me.spec.ts) covers it end
to end: cookie issued only when the box is checked, then auto-login from it once the session cookie is gone.

## Admin

`/admin/actions.html` looks up a user by email and locks or unlocks the account. It is
[`AdminController`](../src/main/java/com/digitalsanctuary/spring/demo/controller/AdminController.java),
guarded by `@PreAuthorize("hasAuthority('ADMIN_PRIVILEGE')")`, and the "Admin Actions" menu item appears
under the same authority in
[`fragments/header.html:48-49`](../src/main/resources/templates/fragments/header.html). The form posts JSON
`{"email": "..."}` to `POST /admin/lockAccount` or `POST /admin/unlockAccount`
([`AdminAPIController`](../src/main/java/com/digitalsanctuary/spring/demo/controller/AdminAPIController.java),
same authority, CSRF required), and renders the returned `JSONResponse` message: 200 on success, 400 for a
blank email, 404 for an unknown one. An admin lock sets the same flag the failed-login lockout uses, so it
expires on the same timer, `user.security.accountLockoutDuration: 30` minutes (`application.yml:147`); set
that to `-1` to make a lock last until an admin unlocks it. Locking does not invalidate a session the user
already has, it blocks the next login.

**Getting an admin user.** The demo seeds no users, only roles: `ROLE_ADMIN`, `ROLE_MANAGER`,
`ROLE_USER` and their privileges are created at startup from `user.roles.roles-and-privileges`
(`application.yml:200-222`), and new registrations get `ROLE_USER`. Register normally, then grant the
role in the database: the framework's tables are `user_account`, `role`, and the join table
`users_roles(user_id, role_id)`.

```sql
INSERT INTO users_roles (user_id, role_id)
SELECT u.id, r.id FROM user_account u JOIN `role` r ON r.name = 'ROLE_ADMIN'
WHERE u.email = 'you@example.com';
```

Get a shell on the right database first: `docker exec -it springuserframeworkdemoapp-mariadb-1 mariadb
-uspringuser -pspringuser springuser` for `./gradlew bootRun`, or the same with `springuser-db` for the
`docker compose up` stack. Log out and back in afterwards, since authorities are loaded at login.

## Troubleshooting

**Passkey registration or login fails.** Check, in order: `user.webauthn.enabled` is `true`
(`application.yml:117`); `rpId` equals the browser's hostname (`localhost` locally); `allowedOrigins`
equals the browser origin exactly, scheme and port included (`http://localhost:8080`, not `https://`,
not `127.0.0.1`); `/webauthn/authenticate/**` and `/login/webauthn` are in `unprotectedURIs`
(`application.yml:160`). Anything other than `localhost` requires HTTPS, so tunnel with
`ngrok http 8080` and update `rpId` and `allowedOrigins` to match. WebAuthn API errors surface in the
browser console, not the server log.

**OAuth2 or OIDC login fails or redirects wrongly.** Check the client ID and secret in
`application-local.yml` (for Keycloak, `DS_SPRING_USER_KEYCLOAK_*` in `keycloak.env`) against what the
provider has, and that the provider's registered callback matches your `redirect-uri`, default shape
`{baseUrl}/login/oauth2/code/{registrationId}`. Google and Facebook will not call back to `localhost`;
tunnel with `ngrok http 8080`. For Keycloak, remember the realm is `demo`, so URLs are
`/realms/demo/...` and the app login is `demo` / `demo`, while `admin` / `admin` only opens the admin
console at http://localhost:8180. A realm re-exported from that console masks the client secret as
`**********`, which then no longer matches `keycloak.env` and breaks login: use the `kc.sh export`
command in [`keycloak/README.md`](../keycloak/README.md). `com.digitalsanctuary: DEBUG` is already on
in the Keycloak profile (`application-docker-keycloak.yml:7-10`); add
`org.springframework.security: DEBUG` to trace the full OIDC exchange.

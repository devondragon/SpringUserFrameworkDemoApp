# Extending the Spring User Framework

This demo depends on `com.digitalsanctuary:ds-spring-user-framework:5.3.0` ([build.gradle](../build.gradle)). Every
section below names one extension point the framework offers, the demo code that uses it, the configuration that wires
it, and what you would write in your own application to do the same.

Framework reference documentation lives in the library repository:
[README.md](https://github.com/devondragon/SpringUserFramework/blob/main/README.md),
[CONFIG.md](https://github.com/devondragon/SpringUserFramework/blob/main/CONFIG.md),
[docs/PROFILE.md](https://github.com/devondragon/SpringUserFramework/blob/main/docs/PROFILE.md),
[docs/REGISTRATION-GUARD.md](https://github.com/devondragon/SpringUserFramework/blob/main/docs/REGISTRATION-GUARD.md).

## Custom user profile stack

The framework owns the `User` entity and authentication. Application-specific user data goes in a profile entity that
shares the user's primary key. The demo implements all five steps of
[docs/PROFILE.md](https://github.com/devondragon/SpringUserFramework/blob/main/docs/PROFILE.md):

| PROFILE.md step | Framework type | Demo class |
| --- | --- | --- |
| 1. Profile entity | `BaseUserProfile` | [DemoUserProfile](../src/main/java/com/digitalsanctuary/spring/demo/user/profile/DemoUserProfile.java) |
| 2. Repository | `JpaRepository` | [DemoUserProfileRepository](../src/main/java/com/digitalsanctuary/spring/demo/user/profile/DemoUserProfileRepository.java) |
| 3. Profile service | `UserProfileService<T>` | [DemoUserProfileService](../src/main/java/com/digitalsanctuary/spring/demo/user/profile/DemoUserProfileService.java) |
| 4. Session holder | `BaseSessionProfile<T>` | [DemoSessionProfile](../src/main/java/com/digitalsanctuary/spring/demo/user/profile/session/DemoSessionProfile.java) |
| 5. Auth listener | `BaseAuthenticationListener<T>` | [DemoAuthenticationListener](../src/main/java/com/digitalsanctuary/spring/demo/user/profile/session/DemoAuthenticationListener.java) |

`DemoUserProfile` is mapped to table `demo_user_profile` and adds `favoriteColor`, `receiveNewsletter`, and a
`@OneToMany` list of [EventRegistration](../src/main/java/com/digitalsanctuary/spring/demo/user/profile/EventRegistration.java)
(table `event_registrations`, with [EventRegistrationRepository](../src/main/java/com/digitalsanctuary/spring/demo/user/profile/EventRegistrationRepository.java)).
`BaseUserProfile` supplies the `@Id`, the `@OneToOne @MapsId` link to `User`, `lastAccessed`, and `locale`, so the
profile row's id is the user's id.

`DemoUserProfileService` implements the two interface methods (`getOrCreateProfile`, `updateProfile`) and adds
domain methods `registerForEvent(Long profileId, Long eventId)` and `unregisterFromEvent(Long profileId, Long eventId)`
that load managed entities inside the transaction. `DemoSessionProfile` adds read helpers over the session-held
profile (`isRegisteredForEvent`, `getFavoriteColor`) plus `refreshProfile()`, which re-reads the profile from the
repository after a write so the session is not stale. `DemoAuthenticationListener` is a constructor-only subclass; the
framework base class loads the profile into the session on successful authentication.

In your app: create the four types with your own field set, keep the profile entity's extra columns out of the
framework's `user_account` table, and let the base authentication listener populate the session. Note that Spring does
not inherit `@Scope` into subclasses: annotate your `BaseSessionProfile` subclass with `@SessionScopedProfile` (or
repeat the explicit `@Scope(SCOPE_SESSION, proxyMode = TARGET_CLASS)`), otherwise it registers as a singleton shared by
every session. `DemoSessionProfile` currently declares only `@Component`, so it is not a model to copy on that point.

## Cleaning up application data when a user is deleted

The framework publishes `com.digitalsanctuary.spring.user.event.UserPreDeleteEvent` inside the deletion transaction,
carrying `userId` and `userEmail` (not a live entity).
[UserProfileDeletionListener](../src/main/java/com/digitalsanctuary/spring/demo/user/profile/UserProfileDeletionListener.java)
handles it with `@EventListener` plus `@Transactional`, looks the profile up by id (same id as the user), and deletes
it; `EventRegistration` rows go with it through `cascade = ALL, orphanRemoval = true` on the profile's collection.

In your app: register one such listener per aggregate that holds a foreign key to the user, and do the work in the
event's transaction so a failed cleanup rolls the deletion back. Whether the account is deleted or only disabled is
controlled by `user.actuallyDeleteAccount` ([application.yml:103](../src/main/resources/application.yml)).

## Building your own domain on the framework: events

The Event feature is the "your application" half of the demo. It is ordinary Spring MVC plus JPA that leans on the
framework only for identity and authorization:

- [Event](../src/main/java/com/digitalsanctuary/spring/demo/event/Event.java) (table `events`) and
  [EventRepository](../src/main/java/com/digitalsanctuary/spring/demo/event/EventRepository.java) /
  [EventService](../src/main/java/com/digitalsanctuary/spring/demo/event/EventService.java).
- [EventAPIController](../src/main/java/com/digitalsanctuary/spring/demo/event/EventAPIController.java): REST under
  `/api/events`. `GET /api/events` and `GET /api/events/{id}` are open; `POST /api/events`,
  `PUT /api/events/{id}`, `DELETE /api/events/{id}`, `POST /api/events/{eventId}/register` and
  `POST /api/events/{eventId}/unregister` each carry a `@PreAuthorize`.
- [EventPageController](../src/main/java/com/digitalsanctuary/spring/demo/event/EventPageController.java): the
  Thymeleaf pages `/event/list.html`, `/event/{eventId}/details.html`, `/event/create.html`, `/event/my-events.html`.
- [AdminController](../src/main/java/com/digitalsanctuary/spring/demo/controller/AdminController.java) gates
  `/admin/actions.html` with `@PreAuthorize("hasAuthority('ADMIN_PRIVILEGE')")`, the same mechanism applied to a page
  rather than an API.

The authorities in those annotations are not hard-coded in Java; they come from the framework's role configuration in
[application.yml:192-214](../src/main/resources/application.yml). `user.roles.roles-and-privileges` grants
`CREATE_EVENT_PRIVILEGE`, `DELETE_EVENT_PRIVILEGE`, and `UPDATE_EVENT_PRIVILEGE` to `ROLE_ADMIN` (lines 200-202) and
`REGISTER_FOR_EVENT_PRIVILEGE` to `ROLE_USER` (line 211). `user.roles.role-hierarchy` (lines 212-214) declares
`ROLE_ADMIN > ROLE_MANAGER > ROLE_USER`, so an admin also holds the user privileges without being granted them twice.
The framework creates the roles and privileges from this configuration at startup.

In your app: define one privilege per action, list it under the roles that should have it, and use
`hasAuthority('YOUR_PRIVILEGE')` in `@PreAuthorize` rather than checking role names. Adding a privilege is then a
configuration change, not a code change. Property reference:
[CONFIG.md](https://github.com/devondragon/SpringUserFramework/blob/main/CONFIG.md) and [CONFIGURATION.md](CONFIGURATION.md).

## Overriding a framework service

[CustomUserEmailService](../src/main/java/com/digitalsanctuary/spring/demo/service/CustomUserEmailService.java) extends
the framework's `UserEmailService` and is annotated `@Service @Primary`, so it replaces the framework bean everywhere it
is injected. It overrides one method, `sendForgotPasswordVerificationEmail`: when
`app.mail.sendPasswordResetEmail` is `false` it creates and persists the reset token but sends no mail, otherwise it
delegates to `super`. The Playwright profile sets that flag to `false`
([application-playwright-test.yml:6-8](../src/main/resources/application-playwright-test.yml)) so E2E tests can read
the token back through the test API instead of an inbox.

In your app: subclass the framework service, add `@Primary`, keep the constructor signature (the parent takes its
collaborators by constructor), override only the methods you need, and call `super` on the rest. The same pattern
applies to any framework `@Service` you want to intercept, for example to route mail through a transactional email
provider.

## Web layer glue

- [DemoTemplateModelAdvice](../src/main/java/com/digitalsanctuary/spring/demo/web/DemoTemplateModelAdvice.java): a
  `@ControllerAdvice` exposing `devOrLocalProfile` as a model attribute. Templates cannot call
  `${@environment.acceptsProfiles(...)}` in the restricted (layout-decorated) Thymeleaf expression context, so the
  boolean is precomputed. This is the place to add any demo-only model attribute that does not come from the
  framework's own `${userSecurity}` advice.
- [LocaleConfiguration](../src/main/java/com/digitalsanctuary/spring/demo/util/LocaleConfiguration.java): a
  `CookieLocaleResolver` defaulting to `Locale.US` plus a `LocaleChangeInterceptor` bound to the `lang` request
  parameter, so `?lang=fr` switches the bundle used by the pages.

In your app: use a `@ControllerAdvice` for cross-cutting view data, and add a locale resolver only if you ship more
than one message bundle.

## Registration guard

[DomainRegistrationGuard](../src/main/java/com/digitalsanctuary/spring/demo/registration/DomainRegistrationGuard.java)
implements the framework's `RegistrationGuard` SPI: `evaluate(RegistrationContext)` returns `RegistrationDecision.allow()`
for `RegistrationSource.OAUTH2` and `OIDC`, and for form or passwordless registration allows only email addresses ending
in `registration.guard.allowed-domain` (default `@example.com`), denying everything else with a message. The bean is
annotated `@Profile("registration-guard")`, so it is inert until that profile is active
(`--spring.profiles.active=local,registration-guard`). See [AUTHENTICATION.md#registration-guard](AUTHENTICATION.md#registration-guard)
for how to run it, and
[docs/REGISTRATION-GUARD.md](https://github.com/devondragon/SpringUserFramework/blob/main/docs/REGISTRATION-GUARD.md)
for the full SPI contract. In your app, one `@Component` implementing the interface is the whole integration: allowlists,
invite codes, and per-source rules all fit in `evaluate`.

## Reference templates, JavaScript, and messages

The framework ships the mail templates but no user-facing HTML; its README points adopters at this repository for the
reference set. What to copy:

- [templates/user/](../src/main/resources/templates/user) : `login.html`, `register.html`, `forgot-password.html`,
  `forgot-password-change.html`, `forgot-password-pending-verification.html`, `update-user.html`,
  `update-password.html`, `delete-account.html`, `registration-complete.html`,
  `registration-pending-verification.html`, `request-new-verification-email.html`, and `mfa/webauthn-challenge.html`.
  Forms post to the framework's `/user/*` endpoints and read URIs from the framework-provided `${userSecurity}` model
  attribute rather than hard-coding paths.
- [templates/layout.html](../src/main/resources/templates/layout.html) and
  [templates/fragments/](../src/main/resources/templates/fragments) (`header.html`, `footer.html`): the layout dialect
  shell, the CSRF meta tags every fetch call reads, and `sec:authorize` driven navigation.
- [templates/mail/](../src/main/resources/templates/mail): `registration-token.html` and `forgot-password-token.html`
  are byte-identical copies of the framework's defaults, placed at the same classpath paths so they take precedence.
  Edit them in place to restyle the emails.
- [static/js/user/](../src/main/resources/static/js/user), one module per page. Endpoints they call:
  `register.js` → `POST /user/registration` and `POST /user/registration/passwordless`; `login.js` → the login form
  action plus passkey sign-in; `forgot-password.js` → `POST /user/resetPassword`; `reset-password.js` →
  `POST /user/savePassword`; `resend-verification.js` → `POST /user/resendRegistrationToken`; `update-user.js` →
  `POST /user/updateUser`; `update-password.js` → `POST /user/updatePassword` and `POST /user/setPassword`;
  `delete-account.js` → `DELETE /user/deleteAccount`; `auth-methods.js` → `GET /user/auth-methods`;
  `webauthn-manage.js` → `GET|PUT|DELETE /user/webauthn/credentials*`, `DELETE /user/webauthn/password`, and
  `GET /user/mfa/status`; `webauthn-register.js` and `webauthn-authenticate.js` → the Spring Security WebAuthn
  endpoints `/webauthn/register/options`, `/webauthn/register`, `/webauthn/authenticate/options`, `/login/webauthn`;
  `mfa-webauthn-challenge.js`, `webauthn-utils.js` are helpers with no endpoints of their own.
- [static/js/shared.js](../src/main/resources/static/js/shared.js) (message and error rendering) and
  [static/js/utils/password-validation.js](../src/main/resources/static/js/utils/password-validation.js) (strength
  meter) are imported by the page modules, so copy them too.
- [messages/messages.properties](../src/main/resources/messages/messages.properties), wired by
  `spring.messages.basename: messages/messages` ([application.yml:71-72](../src/main/resources/application.yml)). The
  framework appends its own bundle after yours, so redefining a framework key here (the file overrides `auth.message.*`,
  `email.*`, and the password-policy messages) replaces the library text.

## Profile-gated test-only endpoints

[TestDataController](../src/main/java/com/digitalsanctuary/spring/demo/test/api/TestDataController.java) exposes
`/api/test/**` (user lookup, create, delete, enable, unlock, verification and password-reset token retrieval, health)
for Playwright, and is annotated `@Profile("playwright-test")` so the bean does not exist otherwise. Its delete
endpoint publishes `UserPreDeleteEvent` itself so framework listeners clean up first.
[TestApiSecurityConfig](../src/main/java/com/digitalsanctuary/spring/demo/test/config/TestApiSecurityConfig.java) adds
an `@Order(1)` `SecurityFilterChain` matching `/api/test/**` that disables CSRF and permits the request only when the
remote address is loopback, denying everything else. Both are activated by the `playwright-test` profile; see
[TESTING.md](TESTING.md).

In your app: pair the `@Profile` on the controller with a dedicated, narrow filter chain, and keep the profile out of
production configuration.

## Configuration-only extension points

These need no code in the demo at all:

- MFA: `user.mfa` ([application.yml:114-125](../src/main/resources/application.yml)) is off by default; the `mfa`
  profile ([application-mfa.yml](../src/main/resources/application-mfa.yml)) turns it on with factors `PASSWORD` and
  `WEBAUTHN` and adds the passkey registration endpoints to the unprotected list so a new user can enroll.
- URL protection: `user.security.defaultAction: deny` plus `user.security.unprotectedURIs`
  ([application.yml:142,152](../src/main/resources/application.yml)) decide what is public; the demo adds its own
  `/event/**` and static paths there.
- Remember-me: `user.security.rememberMe` ([application.yml:143-151](../src/main/resources/application.yml)) enables
  the cookie, with the signing key read from `REMEMBER_ME_KEY` and a random per-start fallback.

Every property above is documented in [CONFIGURATION.md](CONFIGURATION.md) and in the framework's
[CONFIG.md](https://github.com/devondragon/SpringUserFramework/blob/main/CONFIG.md).

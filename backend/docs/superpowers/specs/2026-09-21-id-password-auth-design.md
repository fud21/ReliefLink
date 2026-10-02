# ReliefLink ID/Password Authentication Design

## Purpose

Add first-party ID/password authentication to the existing ReliefLink backend without changing the behavior of welfare-program ingestion, scheduling, or matching. The result must support signup, login, authenticated user lookup, access-token refresh, and logout on Spring Boot 4.0.8 / Spring Security 7 while keeping PostgreSQL schema ownership in Flyway.

## Existing-System Constraints

- Java 17, Spring Boot 4.0.8, Gradle Kotlin DSL, Spring MVC, Spring Data JPA, PostgreSQL, and Flyway remain in use.
- `spring.jpa.hibernate.ddl-auto=validate` remains the production setting.
- Existing V1 and V2 migrations are immutable.
- `POST /api/match` remains public so the current matching flow does not regress.
- The two local-profile admin sync endpoints remain structurally unchanged and public for compatibility with their current local-only behavior.
- Existing welfare clients, scheduler, `BenefitProgram`, `BenefitProgramIngestService`, `MatchController`, and `MatchService` are not refactored.
- External data.go.kr APIs are never called by authentication tests.
- Existing uncommitted changes in the working tree are preserved and incorporated only where they overlap this feature.

## Dependencies and Configuration

Add Spring Security and JJWT 0.13.0 (`jjwt-api`, `jjwt-impl`, and `jjwt-jackson`). Spring Boot continues to manage Spring Security, which resolves to 7.0.7 for the current project version.

JWT configuration is externalized under `jwt`:

- `secret`: required Base64-encoded secret supplied by `JWT_SECRET`; decoded key material must be at least 32 bytes for HS-256.
- `access-token-expiration-ms`: 1,800,000 milliseconds (30 minutes).
- `refresh-token-expiration-ms`: 1,209,600,000 milliseconds (14 days).
- `refresh-cookie-secure`: supplied by `JWT_COOKIE_SECURE`, defaulting to `false` for local HTTP and set to `true` in HTTPS production.

CORS configuration permits the configured frontend origins, defaults to `http://localhost:5173`, and allows credentials so the browser can send the refresh cookie. A deployment may override it with `CORS_ALLOWED_ORIGINS` as a comma-separated list.

No real secret is committed. Root `.env.example` documents `DB_PASSWORD`, `BOKJIRO_SERVICE_KEY`, `JWT_SECRET`, `JWT_COOKIE_SECURE`, and `CORS_ALLOWED_ORIGINS` with empty or safe non-secret example values.

## User Domain

`user` is an independent top-level feature package, not nested under `auth`.

`User` maps to `users` and contains:

- `id BIGSERIAL PRIMARY KEY`
- `login_id VARCHAR(50) NOT NULL UNIQUE`
- `password VARCHAR(100) NOT NULL`
- `name VARCHAR(50) NOT NULL`
- `role VARCHAR(20) NOT NULL`
- `created_at TIMESTAMP NOT NULL`

The entity uses a protected JPA constructor, an explicit signup factory or constructor, getters, and no general-purpose setters. New users receive `UserRole.USER`; supported roles are `USER` and `ADMIN`. Passwords stored in this entity are BCrypt hashes only.

`UserRepository` supports `findByLoginId` and `existsByLoginId`.

## Refresh-Token Domain

`RefreshToken` lives under `auth.domain` and maps to `refresh_tokens`:

- `id BIGSERIAL PRIMARY KEY`
- `user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE`
- `token_hash CHAR(64) NOT NULL UNIQUE`
- `expires_at TIMESTAMP NOT NULL`
- `created_at TIMESTAMP NOT NULL`

Only a SHA-256 hexadecimal hash of the serialized refresh JWT is persisted. The raw token exists only in transit in an HttpOnly cookie. The unique `user_id` constraint intentionally provides one active refresh session per user; a successful login replaces the previous row.

V3 creates `users`. V4 creates `refresh_tokens`. Entity column names, lengths, nullability, enum representation, timestamp types, constraints, and relationships must match these migrations so Hibernate validation succeeds.

## API Contracts

### `POST /api/auth/signup`

Accepts `loginId`, `password`, and `name`.

- `loginId`: nonblank, 4–50 characters.
- `password`: nonblank, 8–100 characters.
- `name`: nonblank, 1–50 characters.

The service checks uniqueness, BCrypt-encodes the password, creates a `USER`, and returns HTTP 201 with safe user fields only. A duplicate ID returns HTTP 409. A database unique-constraint race is also translated to the same conflict response. Validation failures return HTTP 400.

### `POST /api/auth/login`

Accepts nonblank `loginId` and `password`. Missing users and `PasswordEncoder.matches(raw, encoded)` failures both return HTTP 401 with the same public message: `아이디 또는 비밀번호가 올바르지 않습니다.`

Success creates an access JWT and refresh JWT, replaces the user's stored refresh-token hash, writes the refresh JWT to a cookie, and returns:

```json
{
  "accessToken": "...",
  "tokenType": "Bearer",
  "expiresIn": 1800
}
```

The refresh cookie is named `refreshToken`, is HttpOnly, uses `SameSite=Lax`, has path `/api/auth`, takes its `Secure` flag from configuration, and has a 14-day maximum age. The refresh token never appears in JSON.

### `POST /api/auth/refresh`

Reads the refresh cookie. It rejects a missing cookie, bad signature, wrong token type, expiry, missing DB hash, or a token whose subject does not match the stored token's user. Success returns a new access token in the login-response shape. The existing refresh token remains unchanged; rotation is deliberately out of scope for this first implementation.

### `POST /api/auth/logout`

Reads the refresh cookie, removes its matching DB row when present, clears the cookie, and returns a successful empty response. The operation is idempotent and public so a client with an expired access token can still discard its refresh session. It does not add an access-token blacklist; an already issued access token remains usable until its short expiry.

### `GET /api/users/me`

Requires a valid Bearer access token. It returns `id`, `loginId`, `name`, and `role`; it never returns a password. Missing, malformed, forged, expired, or refresh-type bearer tokens receive HTTP 401.

## JWT Design

Both JWT types use HS-256 and the configured secret. Claims contain only the minimum authentication data:

- subject: `loginId`
- `role`: `USER` or `ADMIN`
- `type`: `access` or `refresh`
- standard issued-at and expiration values

`JwtTokenProvider` owns token creation, signature and expiration verification, token-type enforcement, subject extraction, and role extraction. It exposes typed validation results or exceptions so callers can distinguish expired from otherwise invalid tokens without leaking cryptographic details.

`JwtAuthenticationFilter` is a `OncePerRequestFilter` placed before the username/password filter. It ignores requests without a Bearer header. For a valid access token it loads the current user through `CustomUserDetailsService`, creates an authenticated token, and stores it in `SecurityContextHolder`. It never authenticates malformed, expired, forged, or refresh tokens. On protected endpoints, an authentication entry point emits a JSON 401 response.

Although the access token contains a role claim as required, authorities are built from the current database user so disabled/deleted users or future role changes are not authorized solely from stale claims.

## Spring Security Policy

The security filter chain:

- disables form login and HTTP Basic;
- disables servlet sessions with `SessionCreationPolicy.STATELESS`;
- disables CSRF for the stateless Bearer API;
- enables credentialed CORS for configured origins;
- permits signup, login, refresh, logout, `/api/match`, and the existing local admin sync paths;
- requires authentication for `/api/users/me`;
- requires authentication for any otherwise-unlisted endpoint, avoiding accidental exposure of future APIs;
- returns JSON 401 for unauthenticated access and JSON 403 for authenticated users without authority.

`SameSite=Lax` prevents a browser from attaching the refresh cookie to ordinary cross-site POST requests. If frontend and backend are later deployed on different sites and require `SameSite=None`, a dedicated CSRF defense for cookie-authenticated refresh/logout endpoints must be added before that change.

## Components and Package Layout

- `auth.controller.AuthController`: HTTP contracts and refresh-cookie creation/removal.
- `auth.dto`: signup, login, token response, and safe signup response records.
- `auth.service.AuthService`: signup, credential verification, refresh-token persistence/checking, access renewal, and logout.
- `auth.jwt.JwtProperties`, `JwtTokenProvider`, `JwtAuthenticationFilter`: configuration and JWT mechanics.
- `auth.domain.RefreshToken`, `RefreshTokenRepository`: persisted refresh-token hash.
- `user.domain.User`, `UserRole`: user aggregate.
- `user.repository.UserRepository`: user persistence queries.
- `user.service.UserService`: current-user lookup.
- `user.controller.UserController` plus a safe response DTO: `/api/users/me`.
- `security.SecurityConfig`, `CustomUserDetails`, `CustomUserDetailsService`, and JSON security handlers: Spring Security integration.
- `common.exception`: minimal feature-focused exception types, error response, and global exception handler.

No `TokenRefreshRequest` is needed because the selected transport is an HttpOnly cookie.

## Error Model

API failures use a small JSON structure containing a stable error code and message. Expected mappings are:

- invalid request: 400
- duplicate login ID: 409
- invalid credentials: 401
- missing/invalid/expired access token: 401
- missing/invalid/expired/unregistered refresh token: 401
- authenticated but forbidden: 403

Logs may retain exception categories for diagnosis but must not log passwords, raw access tokens, raw refresh tokens, JWT secrets, or token hashes.

## Test Strategy

Development follows red-green-refactor. Tests cover at least:

- signup success, duplicate login ID, BCrypt persistence, and request validation;
- login success, unknown login ID, wrong password, access-token creation, refresh-cookie issuance, and the shared public failure message;
- valid JWT parsing, forged JWT rejection, expired JWT rejection, and refresh-token rejection as an access token;
- `/api/users/me` without JWT returning 401 and with a valid JWT returning the safe current-user response;
- refresh success plus missing, expired, forged, unregistered, and user-mismatched token failures;
- logout removing the stored refresh-token hash and preventing later refresh;
- public access to `/api/match` and preservation of the local admin controller's profile behavior.

Tests use an isolated test configuration and H2 in PostgreSQL compatibility mode. Flyway runs V1 through V4 and Hibernate remains on `ddl-auto=validate`, providing a fast schema/entity consistency check without requiring the developer's PostgreSQL password. Authentication tests do not trigger scheduler work or external HTTP calls.

Final verification is `./gradlew clean build`. If an accessible local PostgreSQL instance is available, application startup is additionally used to confirm PostgreSQL-native Flyway execution; otherwise the H2 compatibility migration test is reported separately from real PostgreSQL verification.

## Explicitly Deferred Work

- Refresh-token rotation and replay-family detection.
- Multiple simultaneous refresh sessions per user/device.
- Immediate access-token revocation or a blacklist.
- Password reset, email verification, account lockout, and rate limiting.
- Production cross-site cookie support; switching to `SameSite=None` requires CSRF protection and `Secure=true`.

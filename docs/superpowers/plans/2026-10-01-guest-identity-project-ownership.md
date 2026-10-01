# Guest Identity and Project Ownership Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give each visitor a durable random account that can be restored in another browser, then use that account to protect project creation, editing, and deletion.

**Architecture:** MySQL stores public account IDs, hashed recovery codes, and hashed browser session tokens. The Spring backend issues same-origin HttpOnly cookies and decides project ownership from the resolved account; the Vue frontend displays the account card and restore flow. Existing `owner_id` projects remain readable and require verified administrative assignment before editing.

**Tech Stack:** Java 17, Spring Boot 3.5, MyBatis, MySQL, JUnit 5/Mockito, Vue 3, Pinia, Axios, Vite.

**Spec:** `docs/superpowers/specs/2026-10-01-anonymous-project-community-design.md`

## Global Constraints

- Public account ID is immutable and is never sufficient to authorize a write.
- Recovery code has at least 160 bits of cryptographic randomness; session token has at least 256 bits. Store only hashes in MySQL; show the raw recovery code only on create or rotate.
- Production browser session uses `HttpOnly; Secure; SameSite=Lax; Path=/api`; dev may omit `Secure` only on localhost/127.0.0.1. Session lifetime is 365 days with renewal on activity.
- Browser A and browser B may use the same recovered account concurrently. Rotating the recovery code invalidates the previous code but leaves existing sessions active.
- API uses same-origin `/api`; public GET stays accessible without a session. State-changing requests use a CSRF header and Origin validation.
- Old browser-generated `ownerId`, `ownerName`, and `X-User-Id` do not grant ownership. Legacy projects stay public and read-only pending admin assignment.
- Preserve the existing uncommitted changes in `address-plat/src/utils/ownership.js` and `address-plat/src/views/AiAssetsView.vue`; inspect the diff before editing either file.

## File Map

Backend: `src/main/resources/migration_guest_identity.sql` adds account/session/rate-limit tables and project column. `entity/GuestAccount.java`, `entity/GuestSession.java`, `mapper/GuestAccountMapper.java`, `mapper/GuestSessionMapper.java`, `mapper/RateLimitMapper.java`, and `service/GuestIdentityService.java` own identity persistence. `controller/GuestIdentityController.java` maps session/profile APIs; `controller/CsrfController.java` exposes the CSRF token. `config/ApiSecurityConfig.java` owns CSRF and Origin rules; `common/UnauthorizedException.java` plus `GlobalExceptionHandler.java` define error responses. Existing `ProjectController.java`, `ProjectService.java`, `ProjectMapper.java`, `Project.java`, and `ProjectDTO.java` are the only project write-path changes.

Frontend: `src/api/client.js` centralizes same-origin Axios and CSRF; `src/api/identity.js` is the account API; `src/store/guestAccount.js` owns profile, first-visit, and restore state; `src/components/AccountPanel.vue` handles nickname, recovery card, and restore. Modify `src/components/LayoutHeader.vue`, `src/components/ProjectForm.vue`, `src/api/project.js`, `src/views/ProjectStudioView.vue`, `src/components/ProjectList.vue`, `src/views/HomeView.vue`, and other project owner-display call sites to use server-returned `canEdit`. Keep `src/utils/userIdentity.js` for the unrelated growth capsule until its migration is designed.

### Task 1: Persist accounts and browser sessions

**Files:** Create `src/main/resources/migration_guest_identity.sql`, `src/main/java/kd/address/view/entity/GuestAccount.java`, `src/main/java/kd/address/view/entity/GuestSession.java`, `src/main/java/kd/address/view/mapper/GuestAccountMapper.java`, `src/main/java/kd/address/view/mapper/GuestSessionMapper.java`, `src/main/java/kd/address/view/mapper/RateLimitMapper.java`; test `src/test/java/kd/address/view/service/GuestIdentityServiceTest.java` begins in Task 2.

**Interfaces:** `GuestAccountMapper.insert(GuestAccount)`, `findByPublicId(String)`, `findByNickname(String)`, `findById(Long)`, `updateNickname(Long,String)`, `updateRecoveryHash(Long,String)`; `GuestSessionMapper.insert(GuestSession)`, `findByTokenHash(String)`, `renew(Long,LocalDateTime)`.

- [ ] **Step 1: Add the migration with unique keys and an additive project column.**

```sql
CREATE TABLE IF NOT EXISTS community_account (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  public_id VARCHAR(16) NOT NULL UNIQUE,
  nickname VARCHAR(20) NOT NULL UNIQUE,
  recovery_hash CHAR(64) NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'active',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS community_session (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  account_id BIGINT NOT NULL,
  token_hash CHAR(64) NOT NULL UNIQUE,
  expires_at DATETIME NOT NULL,
  revoked_at DATETIME NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_account (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS community_rate_limit (
  action_key VARCHAR(128) NOT NULL,
  window_start DATETIME NOT NULL,
  hit_count INT NOT NULL,
  PRIMARY KEY(action_key, window_start)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
ALTER TABLE project ADD COLUMN owner_account_id BIGINT NULL;
CREATE INDEX idx_project_owner_account ON project(owner_account_id);
```

- [ ] **Step 2: Add MyBatis entities and annotated mapper methods.**

```java
@Select("SELECT * FROM community_account WHERE public_id = #{publicId} LIMIT 1")
GuestAccount findByPublicId(@Param("publicId") String publicId);
@Select("SELECT * FROM community_session WHERE token_hash = #{hash} AND revoked_at IS NULL AND expires_at > NOW() LIMIT 1")
GuestSession findByTokenHash(@Param("hash") String hash);
```

- [ ] **Step 3: Validate the migration on a disposable MySQL database and commit.**

```bash
mysql -h 127.0.0.1 -P 13306 -u root -p -e 'CREATE DATABASE IF NOT EXISTS portal_identity_test'
mysql -h 127.0.0.1 -P 13306 -u root -p portal_identity_test < src/main/resources/init_project_table.sql
mysql -h 127.0.0.1 -P 13306 -u root -p portal_identity_test < src/main/resources/migration_guest_identity.sql
mvn -q -DskipTests package
git add src/main/resources/migration_guest_identity.sql src/main/java/kd/address/view/entity/GuestAccount.java src/main/java/kd/address/view/entity/GuestSession.java src/main/java/kd/address/view/mapper/GuestAccountMapper.java src/main/java/kd/address/view/mapper/GuestSessionMapper.java src/main/java/kd/address/view/mapper/RateLimitMapper.java
git commit -m "feat: persist guest identities and sessions"
```

The database command must target a disposable copy, never the live database. Make the `ALTER TABLE` repeatable by checking `information_schema.columns` or by documenting it as a one-time versioned migration; do not rely on repeated `spring.sql.init` to run it.

### Task 2: Create, restore, and rotate the permanent identity

**Files:** Create `src/main/java/kd/address/view/service/GuestIdentityService.java`, `src/main/java/kd/address/view/controller/GuestIdentityController.java`, `src/main/java/kd/address/view/common/UnauthorizedException.java`, `src/test/java/kd/address/view/service/GuestIdentityServiceTest.java`; modify `src/main/java/kd/address/view/common/GlobalExceptionHandler.java`.

**Interfaces:** `record CreatedIdentity(GuestAccount account,String recoveryCode,String sessionToken)` is returned by `GuestIdentityService.create()`; `restore(String publicId,String recoveryCode): String sessionToken`, `resolve(String sessionToken): GuestAccount`, `rotateRecovery(Long accountId): String`, `rename(Long accountId,String nickname): GuestAccount`; controller exposes `GET /api/me`, `PATCH /api/me`, `POST /api/guest-sessions`, `POST /api/guest-sessions/restore`, `POST /api/me/recovery-code/rotate`.

- [ ] **Step 1: Write service tests for recovery across browsers and invalid code.**

```java
@Test void restoreReturnsTheSameAccount() {
  CreatedIdentity created = service.create();
  String secondToken = service.restore(created.account().getPublicId(), created.recoveryCode());
  assertEquals(created.account().getId(), service.resolve(secondToken).getId());
}
@Test void publicIdAloneCannotRestore() {
  CreatedIdentity created = service.create();
  assertThrows(UnauthorizedException.class,
      () -> service.restore(created.account().getPublicId(), ""));
}
```

- [ ] **Step 2: Run the targeted test and observe red.**

```bash
mvn -q -Dtest=GuestIdentityServiceTest test
```

- [ ] **Step 3: Implement cryptographic IDs, hashes, constant-time comparison, session lookup, nickname validation, and controller cookies.**

```java
byte[] secret = new byte[20];
secureRandom.nextBytes(secret);
String recoveryCode = Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
String recoveryHash = HexFormat.of().formatHex(
    MessageDigest.getInstance("SHA-256").digest(recoveryCode.getBytes(StandardCharsets.US_ASCII)));
// Class constant: private static final String[] WORDS = {"星河", "清风", "山海", "微光"};
String nickname = WORDS[secureRandom.nextInt(WORDS.length)]
    + String.format("%04d", secureRandom.nextInt(10000));
```

Define `WORDS` as a checked-in list of short neutral Chinese words; retry a nickname collision against the unique key. The controller returns raw recovery code only in the create/rotate response. `GET /api/me` returns 401 when no valid session exists. Restore returns the same generic 401 for an unknown public ID and a wrong recovery code. Cookie issuance is a private helper shared by create and restore; set `Secure` from trusted deployment configuration rather than an arbitrary request header. Generate the public ID from cryptographic random bytes and retry its unique-key collision. `resolve` renews a valid session's expiry on activity without changing the account ID.

- [ ] **Step 4: Run tests, add HTTP tests for cookie flags and response redaction, then commit.**

```bash
mvn -q -Dtest=GuestIdentityServiceTest,GuestIdentityControllerTest test
git add src/main/java/kd/address/view/service/GuestIdentityService.java src/main/java/kd/address/view/controller/GuestIdentityController.java src/main/java/kd/address/view/common/UnauthorizedException.java src/main/java/kd/address/view/common/GlobalExceptionHandler.java src/test/java/kd/address/view/service/GuestIdentityServiceTest.java src/test/java/kd/address/view/controller/GuestIdentityControllerTest.java
git commit -m "feat: issue and restore durable guest accounts"
```

### Task 3: Protect cookie writes and throttle recovery

**Files:** Modify `pom.xml`, `src/main/java/kd/address/view/config/WebConfig.java`, `src/main/java/kd/address/view/mapper/RateLimitMapper.java`; create `src/main/java/kd/address/view/config/ApiSecurityConfig.java`, `src/main/java/kd/address/view/controller/CsrfController.java`, `src/main/java/kd/address/view/service/IdentityRateLimiter.java`, `src/test/java/kd/address/view/config/ApiSecurityConfigTest.java`.

**Interfaces:** `GET /api/csrf` exposes a Spring CSRF token; Axios sends it as `X-CSRF-TOKEN`. `IdentityRateLimiter.checkIssue(String ip)` allows at most 20 new accounts/IP/hour; `checkRestore(String ip,String publicId)` rejects after 10 failures/public ID/hour or 30 failures/IP/hour using `RateLimitMapper`. Origin allowlist comes from a server property and production requires the portal origin.

- [ ] **Step 1: Test a write without CSRF, a valid same-origin write, and the issue limit.**

```java
mockMvc.perform(post("/api/guest-sessions").header("Origin", portalOrigin))
    .andExpect(status().isForbidden());
mockMvc.perform(post("/api/guest-sessions").with(csrf()).header("Origin", portalOrigin))
    .andExpect(status().isOk());
```

- [ ] **Step 2: Run the focused test and observe red.**

```bash
mvn -q -Dtest=ApiSecurityConfigTest test
```

- [ ] **Step 3: Add Spring Security CSRF repository and exact-origin check; remove wildcard credential CORS.**

```java
http.authorizeHttpRequests(a -> a.anyRequest().permitAll())
    .csrf(c -> c.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()));
@GetMapping("/api/csrf")
Map<String,String> csrf(CsrfToken token) { return Map.of("token", token.getToken()); }
```

Use `spring-boot-starter-security` and `spring-security-test`. Keep application authorization in `GuestIdentityService.requireCurrent` and project service, rather than treating `permitAll` as permission to mutate. Return `CsrfToken` from `/api/csrf` to force token generation. Rate limits must be backed by MySQL or a shared cache for multi-instance deployment, keyed separately by IP and public ID; do not use only in-memory counters. `RateLimitMapper` increments atomically before reading the new hit count:

```sql
INSERT INTO community_rate_limit(action_key,window_start,hit_count) VALUES(#{key},#{windowStart},1)
ON DUPLICATE KEY UPDATE hit_count=hit_count+1;
```

- [ ] **Step 4: Run security and service tests and commit.**

```bash
mvn -q test
git add pom.xml src/main/java/kd/address/view/config/WebConfig.java src/main/java/kd/address/view/config/ApiSecurityConfig.java src/main/java/kd/address/view/controller/CsrfController.java src/main/java/kd/address/view/mapper/RateLimitMapper.java src/main/java/kd/address/view/service/IdentityRateLimiter.java src/test/java/kd/address/view/config/ApiSecurityConfigTest.java
git commit -m "feat: protect guest session writes"
```

### Task 4: Bind project writes to the verified account

**Files:** Modify `src/main/java/kd/address/view/controller/ProjectController.java`, `src/main/java/kd/address/view/service/ProjectService.java`, `src/main/java/kd/address/view/mapper/ProjectMapper.java`, `src/main/java/kd/address/view/entity/Project.java`, `src/main/java/kd/address/view/dto/ProjectDTO.java`; test `src/test/java/kd/address/view/service/ProjectServiceTest.java`.

**Interfaces:** `save(ProjectDTO dto, Long accountId)`, `deleteById(Long projectId, Long accountId)`, `getProjects(String category,String keyword,Long accountId)`; `ProjectDTO` gains `ownerAccountId: Long` and `canEdit: boolean`; reads expose `canEdit` computed for the current account and a current nickname, while legacy owner text stays visible.

- [ ] **Step 1: Test forged owner IDs and legacy ownership.**

```java
@Test void forgedOwnerIdCannotUpdateAnotherAccountsProject() {
  Project existing = new Project();
  existing.setId(9L);
  existing.setOwnerAccountId(41L);
  when(mapper.findById(9L)).thenReturn(existing);
  ProjectDTO forged = new ProjectDTO();
  forged.setId(9L);
  forged.setOwnerId("41");
  assertThrows(SecurityException.class, () -> service.save(forged, 42L));
}
```

- [ ] **Step 2: Run targeted test and observe red.**

```bash
mvn -q -Dtest=ProjectServiceTest test
```

- [ ] **Step 3: Set `owner_account_id` from session and never update it from a DTO.**

```java
if (dto.getId() == null) {
  project.setOwnerAccountId(accountId);
  mapper.insert(project);
} else {
  Project existing = mapper.findById(dto.getId());
  if (existing == null) throw new IllegalArgumentException("项目不存在");
  if (!Objects.equals(existing.getOwnerAccountId(), accountId))
    throw new SecurityException("只能修改自己发布的项目");
  mapper.updateContent(project);
}
```

Controller obtains `accountId` from the verified session for POST and DELETE. Remove `X-User-Id` as an authorization source. `updateContent` SQL updates project fields only, not owner fields. Legacy `owner_account_id IS NULL` fails author edits. Add a separately protected admin mapping operation in the second plan.

- [ ] **Step 4: Run project tests and commit.**

```bash
mvn -q -Dtest=ProjectServiceTest test
git add src/main/java/kd/address/view/controller/ProjectController.java src/main/java/kd/address/view/service/ProjectService.java src/main/java/kd/address/view/mapper/ProjectMapper.java src/main/java/kd/address/view/entity/Project.java src/main/java/kd/address/view/dto/ProjectDTO.java src/test/java/kd/address/view/service/ProjectServiceTest.java
git commit -m "feat: enforce server-side project ownership"
```

### Task 5: Add account and restore UI without disrupting existing work

**Files:** Create `address-plat/src/api/client.js`, `address-plat/src/api/identity.js`, `address-plat/src/store/guestAccount.js`, `address-plat/src/components/AccountPanel.vue`; modify `address-plat/src/components/LayoutHeader.vue`, `address-plat/src/api/project.js`, `address-plat/src/components/ProjectForm.vue`, `address-plat/src/views/ProjectStudioView.vue`, `address-plat/src/components/ProjectList.vue`, `address-plat/src/views/HomeView.vue`, `address-plat/vite.config.js`. Inspect before changing `address-plat/src/utils/ownership.js`.

**Interfaces:** `useGuestAccount()` exposes `profile`, `ready`, `restore(publicId, code)`, `rename(nickname)`, `rotateRecovery()`. `identity.js` exposes `getMe`, `createAccount`, `restoreAccount`, `updateNickname`, `rotateRecoveryCode`.

- [ ] **Step 1: Add a shared API client and account API.**

```js
export const api = axios.create({ baseURL: '/api', timeout: 10000, withCredentials: true })
export const getMe = () => api.get('/me').then((r) => r.data.data)
export const restoreAccount = (publicId, recoveryCode) =>
  api.post('/guest-sessions/restore', { publicId, recoveryCode }).then((r) => r.data.data)
```

Request interceptor first obtains `/csrf`, caches the token for this page session, and sets `X-CSRF-TOKEN` on POST/PATCH/DELETE. Do not log or store recovery code in localStorage, router query, or analytics.

- [ ] **Step 2: Implement first-visit, lost-session, and restore states in a Pinia store.**

```js
const knownId = localStorage.getItem('portal_public_account_id')
const profile = await getMe().catch((error) => error.response?.status === 401 ? null : Promise.reject(error))
if (!profile && !knownId) return createAccount()
if (!profile && knownId) return { needsRestore: true, publicId: knownId }
```

Creation response's recovery code is held only in memory until the user copies/downloads the identity card or closes it. A restore to an existing account warns before switching if the provisional account has authored content; never merge accounts silently. Display profile nickname and public ID in the header/panel; include a clear “使用已有账户” action.

- [ ] **Step 3: Remove client-supplied project ownership and use server `canEdit`.**

```js
export const saveProject = (project) => api.post('/projects', project).then((r) => r.data.data)
export const deleteProject = (id) => api.delete(`/projects/${id}`).then((r) => r.data.data)
const canEditSelected = computed(() => selectedProject.value?.canEdit === true)
```

Inspect and preserve existing uncommitted ownership/AI changes; replace only project authorization calls. Do not rely on the current browser-generated ID or the client-side admin flag. Keep legacy growth-capsule identity behavior until its own migration.

- [ ] **Step 4: Build, lint without autofix, manually verify two browsers, and commit only intended files.**

```bash
npm run build
./node_modules/.bin/eslint . --no-fix --no-cache
git add src/api/client.js src/api/identity.js src/store/guestAccount.js src/components/AccountPanel.vue src/components/LayoutHeader.vue src/api/project.js src/components/ProjectForm.vue src/views/ProjectStudioView.vue src/components/ProjectList.vue src/views/HomeView.vue vite.config.js
git commit -m "feat: restore guest account across browsers"
```

### Task 6: Document deployment and complete the identity gate

**Files:** Modify `README.md`, `DEPLOYMENT.md`; add `src/test/java/kd/address/view/controller/GuestIdentityControllerTest.java` if Task 2 has not already created it.

**Interfaces:** Production must serve frontend and `/api` from one HTTPS origin; identity write routes reject missing CSRF, wrong Origin, unknown session, and forged account ID.

- [ ] **Step 1: Document reverse proxy and environment settings.**

```nginx
location /api/ {
    proxy_pass http://127.0.0.1:8089/api/;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

Document one-time migration order, backup of project data, secure cookie configuration, Origin allowlist, and the required front/backend coordinated release. Do not add deployment secrets to the repository.

- [ ] **Step 2: Run the complete backend and frontend gates.**

```bash
mvn -q test
mvn -q package
```

In `address-plat` run `npm run build` and `./node_modules/.bin/eslint . --no-fix --no-cache`. In two browser profiles: create A, save ID/code, restore in B, rotate in B, confirm old code fails, create project in B, confirm A can edit it and unrelated C cannot. Check an old project stays readable but cannot be edited via a forged `X-User-Id`.

- [ ] **Step 3: Commit documentation and record any deployment preconditions.**

```bash
git add README.md DEPLOYMENT.md
git commit -m "docs: deploy same-origin guest identity"
```

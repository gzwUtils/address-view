# Project Forum Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add boards, searchable project-linked topics, chronological replies, reporting, and responsive community pages to the portal.

**Architecture:** This plan starts after `2026-10-01-guest-identity-project-ownership.md`. MyBatis persists forum content and counts; services resolve authors from the verified guest session and enforce ownership. The Vue community pages load paginated API data and refresh the open topic every 10 seconds without moving the reader's scroll position.

**Tech Stack:** Java 17, Spring Boot 3.5, MyBatis, MySQL, JUnit 5/Mockito, Vue 3, Pinia, Axios, Vite, Element Plus.

**Spec:** `docs/superpowers/specs/2026-10-01-anonymous-project-community-design.md`

## Global Constraints

- Complete the guest identity/project ownership plan first; public account ID is display data, never an author credential.
- Three seeded boards are `project-share`/「项目分享」, `tech-talk`/「技术交流」, and `lounge`/「闲聊」.
- Topic title length is 5–120 characters, body 10–10000; reply body 1–2000. Treat all content as plain text, never as trusted HTML.
- Lists use backend pagination with 1-based `page` and `size` capped at 50. Search covers topic title and body; `projectId` links a visible existing project.
- Replies are flat, chronological floors. Deleting a reply keeps its floor number; displayed reply count excludes deleted/hidden replies.
- Authors may edit/delete their own content; admins may hide content and process reports. Public GET remains anonymous.
- New replies are checked every 10 seconds while a topic is visible. A new-reply badge controls when the page appends them.
- Preserve existing uncommitted frontend changes in `src/utils/ownership.js` and `src/views/AiAssetsView.vue` until reviewed; no client-side admin flag grants backend permissions.

## File Map

Backend: `migration_community_forum.sql` creates boards/topics/replies/reports and an admin-session table. `entity/CommunityBoard.java`, `CommunityTopic.java`, `CommunityReply.java`, `CommunityReport.java`, and matching `mapper/*Mapper.java` are persistence units. `service/TopicService.java`, `ReplyService.java`, `ReportService.java`, and `AdminSessionService.java` own use cases. `controller/CommunityController.java`, `MyDiscussionsController.java`, and `AdminCommunityController.java` own API routes. `common/ConflictException.java`, `TooManyRequestsException.java`, and `GlobalExceptionHandler.java` map errors. Existing `ContentController.java`, `AiAssetController.java`, and `ProjectController.java` gain admin or verified-account checks where required.

Frontend: `src/api/community.js` provides typed-shape request helpers. `CommunityHomeView.vue`, `CommunityBoardView.vue`, `CommunityTopicView.vue`, `MyDiscussionsView.vue`, and `CommunityComposer.vue` render forum flows. `AdminSignInView.vue` and `CommunityReportsView.vue` handle admin moderation. Existing `router/index.js`, `LayoutHeader.vue`, `DashboardView.vue`, `ProjectDetailView.vue`, `ProjectsView.vue`, and `composables/useResourceDiscovery.js` add entry points and search integration.

### Task 1: Persist boards, topics, replies, reports, and admin sessions

**Files:** Create `src/main/resources/migration_community_forum.sql`, `src/main/java/kd/address/view/entity/CommunityBoard.java`, `CommunityTopic.java`, `CommunityReply.java`, `CommunityReport.java`, and `src/main/java/kd/address/view/mapper/CommunityBoardMapper.java`, `CommunityTopicMapper.java`, `CommunityReplyMapper.java`, `CommunityReportMapper.java`, `AdminSessionMapper.java`.

**Interfaces:** Topic mapper exposes `findPage(boardCode,projectId,keyword,sort,offset,size)`, `count(...)`, `findById(Long)`, `insert`, `updateContent`, `softDelete`, and `lockById(Long)`; reply mapper exposes `findPage(topicId,offset,size)`, `countVisible(topicId)`, `insert`, `softDelete`, and `latestVisibleTime(topicId)`.

- [ ] **Step 1: Add the versioned migration and seed the three boards.**

```sql
CREATE TABLE community_board (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(32) NOT NULL UNIQUE,
  name VARCHAR(40) NOT NULL,
  description VARCHAR(255) NOT NULL,
  sort_order INT NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'visible'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT INTO community_board(code,name,description,sort_order) VALUES
('project-share','项目分享','分享项目与使用经验',10),
('tech-talk','技术交流','讨论实现与排障',20),
('lounge','闲聊','团队日常交流',30);
CREATE TABLE community_topic (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  board_id BIGINT NOT NULL,
  author_account_id BIGINT NOT NULL,
  project_id BIGINT NULL,
  title VARCHAR(120) NOT NULL,
  body TEXT NOT NULL,
  reply_count INT NOT NULL DEFAULT 0,
  next_floor INT NOT NULL DEFAULT 2,
  last_reply_time DATETIME NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'visible',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_board_activity (board_id,last_reply_time,id),
  KEY idx_project (project_id), KEY idx_author (author_account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE community_reply (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  topic_id BIGINT NOT NULL,
  author_account_id BIGINT NOT NULL,
  reply_to_id BIGINT NULL,
  floor_no INT NOT NULL,
  body TEXT NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'visible',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_topic_floor (topic_id,floor_no),
  KEY idx_topic_time (topic_id,create_time,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE community_report (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  reporter_account_id BIGINT NOT NULL,
  target_type VARCHAR(16) NOT NULL,
  target_id BIGINT NOT NULL,
  reason VARCHAR(500) NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'open',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_status_time (status,create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- [ ] **Step 2: Add the admin-session table and MyBatis mapper methods.**

```sql
CREATE TABLE community_admin_session (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  token_hash CHAR(64) NOT NULL UNIQUE,
  expires_at DATETIME NOT NULL,
  revoked_at DATETIME NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

Use parameterized MyBatis SQL; escape `LIKE` wildcards from `keyword` before binding so `%` and `_` are searched literally. A `sort=recent` query orders by `COALESCE(last_reply_time,create_time) DESC,id DESC`; `sort=new` orders by `create_time DESC,id DESC`. Query only `status='visible'` for public reads and verify the joined board is visible.

- [ ] **Step 3: Apply migration to a disposable MySQL copy, compile, and commit.**

```bash
mvn -q -DskipTests package
git add src/main/resources/migration_community_forum.sql src/main/java/kd/address/view/entity src/main/java/kd/address/view/mapper
git commit -m "feat: persist community boards and discussions"
```

### Task 2: Create and search project-linked topics

**Files:** Create `src/main/java/kd/address/view/service/TopicService.java`, `src/main/java/kd/address/view/controller/CommunityController.java`, `src/main/java/kd/address/view/controller/MyDiscussionsController.java`, `src/main/java/kd/address/view/dto/TopicSaveRequest.java`, `TopicView.java`, `src/test/java/kd/address/view/service/TopicServiceTest.java`; modify `src/main/java/kd/address/view/common/GlobalExceptionHandler.java`.

**Interfaces:** `TopicService.list(String boardCode,Long projectId,String keyword,String sort,int page,int size): PageResponse<TopicView>`, `mine(Long accountId,int page,int size): PageResponse<TopicView>`, `get(Long id,Long currentAccountId): TopicView`, `create(TopicSaveRequest,Long accountId): TopicView`, `update(Long id,TopicSaveRequest,Long accountId)`, `delete(Long id,Long accountId)`.

- [ ] **Step 1: Test project association, search pagination, and author-only edits.**

```java
@Test void anotherAccountCannotEditTopic() {
  CommunityTopic topic = new CommunityTopic();
  topic.setId(14L);
  topic.setAuthorAccountId(22L);
  topic.setStatus("visible");
  when(topicMapper.findById(14L)).thenReturn(topic);
  assertThrows(SecurityException.class,
      () -> service.update(14L, new TopicSaveRequest("新的标题", "足够长的正文内容", "tech-talk", null), 23L));
}
```

- [ ] **Step 2: Run focused tests and observe red.**

```bash
mvn -q -Dtest=TopicServiceTest test
```

- [ ] **Step 3: Implement topic validation, view mapping, count/page queries, and controller routes.**

```java
@GetMapping("/topics")
ApiResponse<PageResponse<TopicView>> listTopics(
    @RequestParam(required=false) String board,
    @RequestParam(required=false) Long projectId,
    @RequestParam(required=false) String keyword,
    @RequestParam(defaultValue="recent") String sort,
    @RequestParam(defaultValue="1") int page,
    @RequestParam(defaultValue="20") int size) {
  return ApiResponse.success(topicService.list(board, projectId, keyword, sort, page, size));
}
```

Define `TopicSaveRequest` as `record TopicSaveRequest(String title,String body,String boardCode,Long projectId)`. Add `GET /api/community/boards`, `GET /api/community/topics/{id}`, `POST /api/community/topics`, `PATCH /api/community/topics/{id}`, and `DELETE /api/community/topics/{id}`. Board list includes visible-topic count and latest activity. `MyDiscussionsController` exposes `GET /api/me/discussions?page=&size=`, selecting distinct topics authored or replied to by the current account. Resolve the author from the verified session for writes. Reject a project ID that does not exist. Public response includes current author nickname from `community_account`; `canEdit` reflects the caller's account.

- [ ] **Step 4: Run tests and commit.**

```bash
mvn -q -Dtest=TopicServiceTest test
git add src/main/java/kd/address/view/service/TopicService.java src/main/java/kd/address/view/controller/CommunityController.java src/main/java/kd/address/view/controller/MyDiscussionsController.java src/main/java/kd/address/view/dto src/main/java/kd/address/view/common/GlobalExceptionHandler.java src/test/java/kd/address/view/service/TopicServiceTest.java
git commit -m "feat: publish and search forum topics"
```

### Task 3: Add chronological replies and safe refresh

**Files:** Create `src/main/java/kd/address/view/service/ReplyService.java`, `src/main/java/kd/address/view/dto/ReplySaveRequest.java`, `ReplyView.java`, `src/test/java/kd/address/view/service/ReplyServiceTest.java`; modify `CommunityController.java`, `CommunityTopicMapper.java`, `CommunityReplyMapper.java`.

**Interfaces:** `ReplyService.list(topicId,page,size): PageResponse<ReplyView>`, `create(topicId,ReplySaveRequest,accountId): ReplyView`, `update(replyId,body,accountId)`, `delete(replyId,accountId)`, `countAfter(topicId,lastReplyId): long`.

- [ ] **Step 1: Test concurrent floor allocation and soft-delete count.**

```java
@Test void deletionKeepsFloorAndUpdatesVisibleCount() {
  service.delete(31L, 7L);
  verify(replyMapper).softDelete(31L);
  verify(topicMapper).setReplyStats(eq(4L), eq(2), any());
}
```

- [ ] **Step 2: Run the focused test and observe red.**

```bash
mvn -q -Dtest=ReplyServiceTest test
```

- [ ] **Step 3: Lock the topic row, allocate a floor, then update counts in one transaction.**

```java
@Transactional
public ReplyView create(long topicId, ReplySaveRequest request, long accountId) {
  CommunityTopic topic = topicMapper.lockById(topicId); // SELECT ... FOR UPDATE
  if (topic == null || !"visible".equals(topic.getStatus()))
    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "主题不存在");
  int floor = topic.getNextFloor();
  replyMapper.insert(topicId, accountId, request.replyToId(), floor, request.body().trim());
  topicMapper.setNextFloorAndReplyStats(topicId, floor + 1, topic.getReplyCount() + 1, LocalDateTime.now());
  return replyMapper.findByTopicAndFloor(topicId, floor);
}
```

Display the topic opener as floor 1; the first reply receives floor 2. Validate any `replyToId` belongs to the same visible topic. On deletion, recompute visible count and latest visible reply time inside the transaction; never renumber floors. Add `GET /api/community/topics/{id}/replies`, `GET /api/community/topics/{id}/replies/new-count?afterId=`, `POST /api/community/topics/{id}/replies`, `PATCH /api/community/replies/{id}`, and `DELETE /api/community/replies/{id}`.

- [ ] **Step 4: Run tests, exercise concurrent requests on disposable MySQL, and commit.**

```bash
mvn -q -Dtest=ReplyServiceTest test
git add src/main/java/kd/address/view/service/ReplyService.java src/main/java/kd/address/view/controller/CommunityController.java src/main/java/kd/address/view/dto/ReplySaveRequest.java src/main/java/kd/address/view/dto/ReplyView.java src/main/java/kd/address/view/mapper/CommunityTopicMapper.java src/main/java/kd/address/view/mapper/CommunityReplyMapper.java src/test/java/kd/address/view/service/ReplyServiceTest.java
git commit -m "feat: add chronological forum replies"
```

### Task 4: Add reports, moderation, and admin access

**Files:** Create `src/main/java/kd/address/view/service/ReportService.java`, `AdminSessionService.java`, `src/main/java/kd/address/view/controller/AdminCommunityController.java`, `src/main/java/kd/address/view/common/ConflictException.java`, `TooManyRequestsException.java`, `src/test/java/kd/address/view/service/ReportServiceTest.java`, `address-plat/src/views/AdminSignInView.vue`, `address-plat/src/views/CommunityReportsView.vue`; modify `ContentController.java`, `AiAssetController.java`, `ProjectController.java`, `GlobalExceptionHandler.java`, `ApiSecurityConfig.java`, `address-plat/src/router/index.js`, and `address-plat/src/components/LayoutHeader.vue`.

**Interfaces:** `ReportService.submit(accountId,targetType,targetId,reason)`, `listOpen(page,size)`, `resolve(reportId,hideTarget,adminSession)`; `AdminSessionService.login(password): token`, `requireAdmin(request): void`.

- [ ] **Step 1: Test that a guest cannot moderate or mutate admin content.**

```java
@Test void guestSessionCannotHideAReportedTopic() {
  assertThrows(SecurityException.class, () -> reportService.resolve(12L, true, "guest-token"));
}
```

- [ ] **Step 2: Run focused test and observe red.**

```bash
mvn -q -Dtest=ReportServiceTest test
```

- [ ] **Step 3: Implement separate admin sessions and report transitions.**

```java
@PostMapping("/session")
ApiResponse<Void> login(@RequestBody AdminPassword request, HttpServletResponse response) {
  String token = adminSessionService.login(request.password());
  response.addHeader("Set-Cookie", adminSessionService.cookie(token));
  return ApiResponse.success(null);
}
record AdminPassword(String password) {}
```

Compare password to a server-configured password hash; fail closed if configuration is absent. Admin cookie is HttpOnly/Secure/SameSite=Lax and separate from guest cookie. `POST /api/community/reports` validates target, length, and rate limits; `GET /api/admin/community/reports` and `PATCH /api/admin/community/reports/{id}` require admin session. When hiding a reply, refresh its topic count. Guard existing content/AI write endpoints with admin session before public launch. If admin manually assigns a legacy project, expose a protected `PATCH /api/admin/projects/{id}/owner` accepting an existing account's public ID; audit the assignment.

Create `/admin/sign-in` with a password form and `/admin/community/reports` with open reports, hide/dismiss actions, and loading/error states. The existing management links may remain visible but their protected actions route to sign-in on 401/403; never read `src/utils/ownership.js`'s client-side admin flag as a server credential.

- [ ] **Step 4: Add account/IP rate limits and error mappings, run tests, commit.**

```bash
mvn -q test
git add src/main/java/kd/address/view/service/ReportService.java src/main/java/kd/address/view/service/AdminSessionService.java src/main/java/kd/address/view/controller/AdminCommunityController.java src/main/java/kd/address/view/controller/ContentController.java src/main/java/kd/address/view/controller/AiAssetController.java src/main/java/kd/address/view/controller/ProjectController.java src/main/java/kd/address/view/config/ApiSecurityConfig.java src/main/java/kd/address/view/common src/test/java/kd/address/view/service/ReportServiceTest.java
git commit -m "feat: moderate community content with admin session"
```

In `address-plat`, run `npm run build` and `./node_modules/.bin/eslint . --no-fix --no-cache`, then commit `src/views/AdminSignInView.vue`, `src/views/CommunityReportsView.vue`, `src/router/index.js`, and `src/components/LayoutHeader.vue` with message `feat: add community moderation console`.

Enforce initial limits from the spec: 5 topics/account/hour, 30 replies/account/hour, 5 nickname changes/account/day, 10 reports/account/day; add IP limits as well. Map invalid input to 400, no session to 401, non-author to 403, missing/hidden content to 404, state conflicts to 409, and rate limit to 429.

### Task 5: Build community browsing and discussion pages

**Files:** Create `address-plat/src/api/community.js`, `src/views/CommunityHomeView.vue`, `CommunityBoardView.vue`, `CommunityTopicView.vue`, `MyDiscussionsView.vue`, `src/components/CommunityComposer.vue`; modify `src/router/index.js`, `src/components/LayoutHeader.vue`, `src/components/AccountPanel.vue`.

**Interfaces:** `listBoards`, `listTopics`, `getTopic`, `listReplies`, `countNewReplies`, `getMyDiscussions`, `createTopic`, `createReply`, `updateTopic`, `deleteTopic`, `updateReply`, `deleteReply`, `reportContent`; routes `/community`, `/community/boards/:code`, `/community/topics/:id`, `/community/mine`.

- [ ] **Step 1: Add API helpers with pagination and no demonstration-data fallback.**

```js
export const listTopics = (params) => api.get('/community/topics', { params }).then((r) => r.data.data)
export const listReplies = (topicId, page, size = 20) =>
  api.get(`/community/topics/${topicId}/replies`, { params: { page, size } }).then((r) => r.data.data)
export const createReply = (topicId, payload) =>
  api.post(`/community/topics/${topicId}/replies`, payload).then((r) => r.data.data)
export const getMyDiscussions = (page = 1) =>
  api.get('/me/discussions', { params: { page, size: 20 } }).then((r) => r.data.data)
```

- [ ] **Step 2: Add community routes and board/topic pages.**

```js
{ path: 'community', name: 'community', component: CommunityHomeView },
{ path: 'community/boards/:code', name: 'community-board', component: CommunityBoardView },
{ path: 'community/topics/:id', name: 'community-topic', component: CommunityTopicView },
{ path: 'community/mine', name: 'my-discussions', component: MyDiscussionsView },
```

Board page keeps `keyword`, `sort`, and `page` in URL query. Topic page renders plain text using Vue interpolation with CSS `white-space: pre-wrap`; detected URLs are sanitized and use `rel="noopener noreferrer"`. Composer retains draft on API failure and shows 400/401/403/429 messages. Author controls use API `canEdit` for visibility, but backend remains authoritative.

- [ ] **Step 3: Poll for new replies only while topic is visible.**

```js
const timer = window.setInterval(async () => {
  if (document.visibilityState !== 'visible') return
  newCount.value = await countNewReplies(topicId.value, lastSeenReplyId.value)
}, 10000)
onBeforeUnmount(() => window.clearInterval(timer))
```

Clicking the badge fetches the missing page(s) and scrolls to the first new floor; polling itself does not move the viewport. Stop timer on route change/unmount and show a manual refresh control.

- [ ] **Step 4: Build and lint without autofix, then commit only these frontend files.**

```bash
npm run build
./node_modules/.bin/eslint . --no-fix --no-cache
git add src/api/community.js src/views/CommunityHomeView.vue src/views/CommunityBoardView.vue src/views/CommunityTopicView.vue src/views/MyDiscussionsView.vue src/components/CommunityComposer.vue src/router/index.js src/components/LayoutHeader.vue src/components/AccountPanel.vue
git commit -m "feat: add forum browsing and discussion UI"
```

### Task 6: Connect community to project discovery and verify the complete flow

**Files:** Modify `address-plat/src/views/DashboardView.vue`, `src/views/ProjectDetailView.vue`, `src/views/ProjectsView.vue`, `src/composables/useResourceDiscovery.js`; update backend `README.md`, `DEPLOYMENT.md`; test backend API with `src/test/java/kd/address/view/controller/CommunityControllerTest.java`.

**Interfaces:** Project detail reads `listTopics({projectId})`; `/explore?type=topic&keyword=...` searches community topics; dashboard displays latest three topics from the real API.

- [ ] **Step 1: Add a failing API test for topic search by title/body and project ID.**

```java
mockMvc.perform(get("/api/community/topics").param("projectId", "7").param("keyword", "订单"))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.data.records[0].projectId").value(7));
```

- [ ] **Step 2: Connect project and discovery entry points.**

```js
const discussionLink = (projectId) => ({
  path: '/community/boards/project-share',
  query: { projectId }
})
```

Project detail shows linked topics and “发起讨论” preselects the project; dashboard shows recent topics; resource discovery gains a `topic` filter and genuine topic counts. Reuse the backend paged response, not a client-side copy of all topics. Do not display fake forum data on API failure.

- [ ] **Step 3: Run backend/frontend gates and two-browser walkthrough.**

```bash
mvn -q test
mvn -q package
```

In `address-plat` run `npm run build` and `./node_modules/.bin/eslint . --no-fix --no-cache`. Browser A publishes a project-linked topic and replies; browser B restores A with ID/code, opens “我的讨论”, continues replying, changes nickname, and sees the same history. Browser C cannot edit A's content. Also verify paging, mobile layout, report submission, admin hide, and 10-second new-reply badge.

- [ ] **Step 4: Update operating docs and commit the verified integration.**

```bash
git add README.md DEPLOYMENT.md src/test/java/kd/address/view/controller/CommunityControllerTest.java
git commit -m "docs: operate the project forum"
```

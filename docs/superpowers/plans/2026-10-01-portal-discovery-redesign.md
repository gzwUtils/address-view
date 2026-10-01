# Portal Discovery Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Make the portal home and resource plaza useful for finding real projects, articles, AI assets, and life content on desktop and mobile.

**Architecture:** Keep the existing Vue 3 routes and Spring Boot resource model. Add optional project keyword filtering in the backend; use a focused frontend discovery module to query the existing project and resource endpoints. Render home and browse views from real responses, with independent error states for resource types.

**Tech Stack:** Java 17, Spring Boot 3.5, MyBatis, Vue 3, Vue Router 4, Element Plus, Vite 7.

**Spec:** `docs/superpowers/specs/2026-10-01-portal-discovery-redesign-design.md`

## Global Constraints

- Preserve existing detail and editor routes and the frontend user's uncommitted `ownership.js` and `AiAssetsView.vue` changes.
- Public browsing does not silently substitute local sample data when an API request fails.
- Search state uses `keyword`, `type`, `page`, and `category` in the URL; `category` applies only to projects.
- Keep project results compatible with the existing list response; content results use the existing `records/total/page/size` page response.
- Preserve the existing dark visual identity while reducing decorative copy and making mobile navigation, keyboard focus, empty states, and retry actions usable.

---

## File structure

Backend `ProjectController` and `ProjectService` own optional project keyword filtering over the existing mapper result. A focused `ProjectServiceTest` verifies matching and unchanged empty-keyword behavior.

Frontend `src/api/project.js` and `src/api/content.js` expose strict read methods. New `src/composables/useResourceDiscovery.js` owns grouped requests, page normalization, and URL query parsing. New `src/components/DiscoveryCard.vue` renders semantic links for each result kind. `ProjectsView.vue` owns browse controls; `DashboardView.vue` owns the home composition; `LayoutHeader.vue` owns desktop and mobile navigation. `App.vue` owns shared colors and focus styles.

### Task 1: Project keyword API

**Files:** Modify `src/main/java/kd/address/view/controller/ProjectController.java`, `service/ProjectService.java`, `pom.xml`; create `src/test/java/kd/address/view/service/ProjectServiceTest.java`.

**Interfaces:** `GET /api/projects?category=&keyword=` returns `ApiResponse<List<ProjectDTO>>`; `ProjectService.getProjects(String category, String keyword)` returns the filtered list. Existing callers without a keyword receive the old result.

- [x] Add a focused JUnit test that mocks `ProjectMapper.findAll()`, checks a name/description hit with case insensitive keyword, and checks that an empty keyword returns every item.
- [x] Run `mvn -q -Dtest=ProjectServiceTest test`; confirm the test fails before implementation.
- [x] Implement controller query binding and service filtering over the mapper result. Match `projectName`, `shortName`, `description`, `category`, `type`, and `platformUrl` after `Locale.ROOT` lowercasing; treat null fields as empty strings. Keep mapper queries unchanged, since this portal currently returns a list without pagination.
- [x] Run `mvn -q -Dtest=ProjectServiceTest test` and `mvn -q -DskipTests package`.

```java
// ProjectServiceTest: Project with projectName "订单中心" must match keyword "订单".
Project project = new Project();
project.setProjectName("订单中心");
when(mapper.findAll()).thenReturn(List.of(project));
assertEquals(1, service.getProjects(null, "订单").size());
assertEquals(1, service.getProjects(null, "").size());

@GetMapping
public ApiResponse<List<ProjectDTO>> getProjects(@RequestParam(required = false) String category,
                                                  @RequestParam(required = false) String keyword) {
    return ApiResponse.success(projectService.getProjects(category, keyword));
}
```

### Task 2: Strict frontend discovery data layer

**Files:** Modify `address-plat/src/api/project.js`, `address-plat/src/api/content.js`; create `address-plat/src/composables/useResourceDiscovery.js`.

**Interfaces:** `getProjects(category, keyword)` keeps the old optional category argument. `getContentResourcePage(params)` returns `{records,total,page,size}` and rejects API failures. `fetchDiscoveryGroup(kind, options)` returns `{records,total,page,size}`. `fetchAllDiscoveryGroups(keyword)` returns independent `{data,error}` results keyed by `project/article/ai/life`. `readDiscoveryQuery(query)` clamps page to at least 1 and normalizes type.

- [x] Add the strict content page reader without using the existing sample fallback, and pass optional keyword through the project reader.
- [x] Add the discovery module. For projects, request `getProjects(category, keyword)`, filter the returned list again for older servers that ignore the keyword, then slice for local pagination. For content, call `/content/resources` with `type=kind`, keyword, page, and size. Use `Promise.allSettled` for the four groups in the all view.
- [x] Normalize malformed/missing page fields to safe empty values; retain individual failures for visible retry controls.
- [x] Run `npm run build` from `address-plat` to check module imports.

```js
export const getContentResourcePage = (params = {}) =>
  api.get('/content/resources', { params }).then((res) => res.data?.data ?? res.data)
```

### Task 3: Resource plaza

**Files:** Create `address-plat/src/components/DiscoveryCard.vue`; replace `address-plat/src/views/ProjectsView.vue`.

**Interfaces:** `DiscoveryCard` receives `kind` and `item`, renders a router link to `/project/:id` or `/explore/:kind/:id`; `ProjectsView` calls the Task 2 discovery functions.

- [x] Build a semantic result card with a visible action and no public edit/delete overlay.
- [x] Build an all view with four grouped previews and result counts; build type views with page controls. Project view includes a category filter from `/projects/categories`.
- [x] Read and update `keyword`, `type`, `page`, and `category` through Vue Router. Clear page on type/category changes; preserve keyword. Search submission uses the same URL route as the header.
- [x] Add independent loading, empty, failure, and retry states. A failed group must not hide successful groups.
- [x] Check desktop/mobile layouts and run `npm run build`.

```js
const openType = (type) => router.push({ path: '/explore', query: { ...route.query, type, page: 1 } })
```

### Task 4: Actionable home

**Files:** Replace `address-plat/src/views/DashboardView.vue`.

**Interfaces:** Home reads `getProjects()`, three strict content page queries, and `getRecentViews(getClientId())`. Result cards reuse `DiscoveryCard`.

- [x] Replace the concept hero with an action oriented search field and four resource shortcuts.
- [x] Show the first six project results in current ID descending order; show content recommendations in `sort_order` order; show recent history only when returned.
- [x] Use real result totals and visible failure/retry states. Do not display static sample totals or claims of recency from `sort_order`.
- [x] Check desktop/mobile layouts and run `npm run build`.

```js
router.push({ path: '/explore', query: keyword.trim() ? { keyword: keyword.trim() } : {} })
```

### Task 5: Navigation and shared visual system

**Files:** Modify `address-plat/src/components/LayoutHeader.vue`, `address-plat/src/App.vue`, and if necessary `address-plat/src/views/LayoutView.vue`.

**Interfaces:** Existing routes remain unchanged. Primary navigation has home, explore, and growth capsule; management menu links to operations, project, AI, and content studios.

- [x] Add a keyboard and touch usable management menu plus a mobile navigation toggle; expose `aria-expanded`, labels, and visible focus styles.
- [x] Keep global search visible on desktop and accessible from mobile, using the same `/explore?keyword=` route.
- [x] Refine typography, contrast, spacing, and card colors; remove narrow screen overflow without removing important actions.
- [x] Run `npm run build` and `./node_modules/.bin/eslint . --no-fix` so checking does not edit unrelated files.

### Task 6: End to end verification

**Files:** Fix only defects found in Tasks 1–5.

- [x] Run backend tests/package and frontend build/lint.
- [x] Verify project only, article only, and AI only searches; URL refresh; type and category page resets; partial failure and retry; detail navigation; mobile menu; keyboard focus.
- [x] Review diffs to confirm the user's existing frontend edits are unchanged and no generated build artifacts are staged.

```bash
mvn -q test
npm run build
```

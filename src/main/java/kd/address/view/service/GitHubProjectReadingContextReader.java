package kd.address.view.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kd.address.view.entity.ExternalProject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class GitHubProjectReadingContextReader implements ProjectReadingContextReader {
    private static final String API = "https://api.github.com";
    private static final Pattern ROOT_MANIFEST = Pattern.compile("(?i)^(pom\\.xml|package\\.json|pyproject\\.toml|go\\.mod|cargo\\.toml|requirements\\.txt|composer\\.json)$");
    private static final int PER_FILE_LIMIT = 9_000;
    private static final int TOTAL_CONTEXT_LIMIT = 36_000;
    private static final int MAX_GITHUB_JSON_BYTES = 8 * 1024 * 1024;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    private final ObjectMapper json;
    private final String token;

    public GitHubProjectReadingContextReader(ObjectMapper json, @Value("${portal.github.token:}") String token) {
        this.json = json;
        this.token = token;
    }

    @Override
    public boolean supports(ExternalProject project) {
        return project != null && "github_search".equals(project.getSourceType());
    }

    @Override
    public ProjectReadingContext read(ExternalProject project) {
        if (!supports(project)) throw new IllegalArgumentException("当前还没有为这个来源配置代码资料读取器");
        RepoPath repo = parseRepo(project.getSourceUrl());
        try {
            JsonNode repository = getJson(API + "/repos/" + repo.owner() + "/" + repo.name());
            String branch = repository.path("default_branch").asText("main");
            JsonNode commit = getJson(API + "/repos/" + repo.owner() + "/" + repo.name()
                    + "/commits/" + encodePath(branch));
            String version = commit.path("sha").asText("");
            String treeSha = commit.path("commit").path("tree").path("sha").asText("");
            if (version.isBlank() || treeSha.isBlank()) throw new IOException("无法确认仓库当前版本");
            JsonNode tree = getJson(API + "/repos/" + repo.owner() + "/" + repo.name()
                    + "/git/trees/" + encodePath(treeSha) + "?recursive=1");
            List<String> paths = new ArrayList<>();
            JsonNode entries = tree.path("tree");
            if (entries.isArray()) {
                for (JsonNode entry : entries) {
                    String path = entry.path("path").asText("");
                    if ("blob".equals(entry.path("type").asText()) && !path.isBlank()) paths.add(path);
                }
            }
            if (paths.isEmpty()) throw new IllegalStateException("仓库文件目录为空或无法读取");

            List<String> selectedPaths = selectPaths(paths);
            List<ProjectReadingSource> sources = new ArrayList<>();
            int remaining = TOTAL_CONTEXT_LIMIT;
            for (String path : selectedPaths) {
                if (remaining <= 0) break;
                String content = getRaw(repo, version, path, Math.min(PER_FILE_LIMIT, remaining));
                if (content.isBlank()) continue;
                String url = "https://github.com/" + repo.owner() + "/" + repo.name() + "/blob/"
                        + version + "/" + encodePathForBrowser(path);
                sources.add(new ProjectReadingSource(path, path, path, url, content));
                remaining -= content.length();
            }
            if (sources.isEmpty()) throw new IllegalStateException("仓库中没有可读取的 README、文档或项目入口文件");
            String description = project.getDescription() == null ? "" : project.getDescription();
            String license = project.getLicenseSpdx() == null ? "" : project.getLicenseSpdx();
            return new ProjectReadingContext(project.getFullName(), description, project.getSourceUrl(),
                    branch, version, project.getLanguage(), license, sources);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("项目资料读取已中断", exception);
        } catch (IOException exception) {
            String detail = exception.getMessage();
            throw new IllegalStateException("读取 GitHub 项目资料失败"
                    + (detail == null || detail.isBlank() ? "" : "：" + detail), exception);
        }
    }

    private JsonNode getJson(String url) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(8))
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .header("User-Agent", "Zaichang-Project-Reader")
                .GET();
        if (!token.isBlank()) builder.header("Authorization", "Bearer " + token);
        HttpResponse<byte[]> response = http.send(builder.build(),
                BoundedBodyHandlers.rejectOversize(MAX_GITHUB_JSON_BYTES));
        if (response.statusCode() < 200 || response.statusCode() >= 300)
            throw new IOException("GitHub 返回 HTTP " + response.statusCode());
        return json.readTree(response.body());
    }

    private String getRaw(RepoPath repo, String version, String path, int limit) throws IOException, InterruptedException {
        String url = API + "/repos/" + repo.owner() + "/" + repo.name() + "/contents/"
                + encodePathForBrowser(path) + "?ref=" + encodePath(version);
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(8))
                .header("Accept", "application/vnd.github.raw+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .header("User-Agent", "Zaichang-Project-Reader").GET();
        if (!token.isBlank()) builder.header("Authorization", "Bearer " + token);
        HttpRequest request = builder.build();
        HttpResponse<byte[]> response = http.send(request, BoundedBodyHandlers.truncate(Math.max(1, limit)));
        if (response.statusCode() < 200 || response.statusCode() >= 300)
            throw new IOException("GitHub 文件接口返回 HTTP " + response.statusCode() + "：" + path);
        return new String(response.body(), StandardCharsets.UTF_8);
    }

    private static List<String> selectPaths(List<String> paths) {
        Set<String> selected = new LinkedHashSet<>();
        paths.stream().filter(path -> path.matches("(?i)^readme(?:\\.[^/]+)?$"))
                .findFirst().ifPresent(selected::add);
        paths.stream().filter(path -> !path.contains("/") && ROOT_MANIFEST.matcher(path).matches())
                .sorted().limit(2).forEach(selected::add);
        paths.stream().filter(path -> path.toLowerCase().startsWith("docs/") && path.toLowerCase().endsWith(".md"))
                .sorted(Comparator.comparingInt(GitHubProjectReadingContextReader::docPriority).thenComparing(String::compareTo))
                .limit(3).forEach(selected::add);
        paths.stream().filter(GitHubProjectReadingContextReader::isEntryPoint)
                .sorted().findFirst().ifPresent(selected::add);
        return selected.stream().limit(7).toList();
    }

    private static int docPriority(String path) {
        String lower = path.toLowerCase();
        if (lower.contains("architecture") || lower.contains("design")) return 0;
        if (lower.contains("getting-started") || lower.contains("quickstart") || lower.contains("overview")) return 1;
        return 2;
    }

    private static boolean isEntryPoint(String path) {
        String lower = path.toLowerCase();
        return lower.endsWith("application.java") || lower.endsWith("/main.rs") || lower.endsWith("/main.py")
                || lower.equals("main.py") || lower.equals("src/index.ts") || lower.equals("src/index.js")
                || lower.equals("src/main.ts") || lower.equals("src/main.js") || lower.equals("app.py");
    }

    private static RepoPath parseRepo(String sourceUrl) {
        URI uri;
        try { uri = URI.create(sourceUrl); }
        catch (Exception exception) { throw new IllegalArgumentException("项目来源地址格式无效"); }
        if (!"https".equalsIgnoreCase(uri.getScheme()) || !"github.com".equalsIgnoreCase(uri.getHost())
                || uri.getRawUserInfo() != null || uri.getRawQuery() != null || uri.getRawFragment() != null
                || (uri.getPort() != -1 && uri.getPort() != 443))
            throw new IllegalArgumentException("当前第一版的代码资料读取仅支持 GitHub 仓库");
        String[] parts = uri.getPath().replaceAll("^/|/$", "").split("/");
        if (parts.length != 2 || !parts[0].matches("[A-Za-z0-9_.-]+") || !parts[1].matches("[A-Za-z0-9_.-]+")
                || ".".equals(parts[0]) || "..".equals(parts[0]) || ".".equals(parts[1]) || "..".equals(parts[1]))
            throw new IllegalArgumentException("无法从来源地址识别 GitHub 仓库");
        String name = parts[1].replaceFirst("\\.git$", "");
        return new RepoPath(parts[0], name);
    }

    private static String encodePath(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String encodePathForBrowser(String value) {
        return java.util.Arrays.stream(value.split("/", -1)).map(GitHubProjectReadingContextReader::encodePath)
                .collect(java.util.stream.Collectors.joining("/"));
    }

    private record RepoPath(String owner, String name) {}
}

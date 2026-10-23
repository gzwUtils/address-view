package kd.address.view.service;

import kd.address.view.entity.ExternalProject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Component
public class GitHubProjectReadingContextReader implements ProjectReadingContextReader {
    private static final Pattern REPO_SEGMENT = Pattern.compile("[A-Za-z0-9_.-]+");
    private static final String VERSION_REF = "HEAD";
    private static final int PER_FILE_LIMIT = 9_000;
    private static final int TOTAL_CONTEXT_LIMIT = 36_000;
    private static final Duration CONTEXT_TIMEOUT = Duration.ofSeconds(60);
    private static final int CONNECT_TIMEOUT_MILLIS = 5_000;
    private static final int READ_TIMEOUT_MILLIS = 8_000;

    private final String readingProxyBaseUrl;

    public GitHubProjectReadingContextReader(
            @Value("${portal.github.reading-proxy-base-url:https://ghproxy.net/}") String readingProxyBaseUrl) {
        this.readingProxyBaseUrl = readingProxyBaseUrl == null ? "" : readingProxyBaseUrl.trim();
    }

    @Override
    public boolean supports(ExternalProject project) {
        return project != null && "github_search".equals(project.getSourceType());
    }

    @Override
    public ProjectReadingContext read(ExternalProject project) {
        if (!supports(project)) throw new IllegalArgumentException("当前还没有为这个来源配置代码资料读取器");
        RepoPath repo = parseRepo(project.getSourceUrl());
        long deadline = System.nanoTime() + CONTEXT_TIMEOUT.toNanos();

        List<ProjectReadingSource> sources = new ArrayList<>();
        int remaining = TOTAL_CONTEXT_LIMIT;
        try {
            remaining = addFirstAvailable(repo, readmePaths(), sources, remaining, deadline);
            remaining = addFirstAvailable(repo, manifestPaths(project.getLanguage()), sources, remaining, deadline);
            remaining = addFirstAvailable(repo, documentationPaths(), sources, remaining, deadline);
            remaining = addFirstAvailable(repo, entryPointPaths(project.getLanguage()), sources, remaining, deadline);
            addFirstAvailable(repo, List.of("CONTRIBUTING.md"), sources, remaining, deadline);
        } catch (IOException exception) {
            String detail = exception.getMessage();
            throw new IllegalStateException("读取 GitHub 项目资料失败"
                    + (detail == null || detail.isBlank() ? "" : "：" + detail), exception);
        }

        if (sources.isEmpty()) throw new IllegalStateException("仓库中没有可读取的 README、项目清单或文档");
        String description = project.getDescription() == null ? "" : project.getDescription();
        String license = project.getLicenseSpdx() == null ? "" : project.getLicenseSpdx();
        return new ProjectReadingContext(project.getFullName(), description, project.getSourceUrl(),
                "default", sourceFingerprint(sources), project.getLanguage(), license, sources);
    }

    private int addFirstAvailable(RepoPath repo, List<String> candidates, List<ProjectReadingSource> sources,
                                  int remaining, long deadline) throws IOException {
        if (remaining <= 0) return remaining;
        for (String path : candidates) {
            String content = readRawFile(repo, path, Math.min(PER_FILE_LIMIT, remaining), deadline);
            if (content.isBlank()) continue;
            String url = "https://github.com/" + repo.owner() + "/" + repo.name() + "/blob/"
                    + VERSION_REF + "/" + encodePathForBrowser(path);
            sources.add(new ProjectReadingSource(path, path, path, url, content));
            return remaining - content.length();
        }
        return remaining;
    }

    private String readRawFile(RepoPath repo, String path, int limit, long deadline) throws IOException {
        String rawUrl = "https://raw.githubusercontent.com/" + repo.owner() + "/" + repo.name()
                + "/" + VERSION_REF + "/" + encodePathForBrowser(path);
        String requestUrl = readingProxyBaseUrl.isBlank() ? rawUrl : proxiedUrl(rawUrl);
        int timeoutMillis = remainingMillis(deadline);
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) URI.create(requestUrl).toURL().openConnection();
            connection.setConnectTimeout(Math.min(CONNECT_TIMEOUT_MILLIS, timeoutMillis));
            connection.setReadTimeout(Math.min(READ_TIMEOUT_MILLIS, timeoutMillis));
            connection.setInstanceFollowRedirects(false);
            connection.setUseCaches(false);
            connection.setRequestProperty("Accept", "text/plain, */*;q=0.8");
            int status = connection.getResponseCode();
            if (status == HttpURLConnection.HTTP_NOT_FOUND) return "";
            if (status != HttpURLConnection.HTTP_OK)
                throw new IOException("GitHub 原始文件读取 HTTP " + status + "：" + path);
            try (InputStream input = connection.getInputStream();
                 ByteArrayOutputStream output = new ByteArrayOutputStream(Math.min(limit, 4096))) {
                byte[] buffer = new byte[4096];
                int read;
                while (output.size() < limit && (read = input.read(buffer, 0, Math.min(buffer.length, limit - output.size()))) != -1) {
                    output.write(buffer, 0, read);
                }
                return output.toString(StandardCharsets.UTF_8);
            }
        } catch (SocketTimeoutException exception) {
            throw new IOException("GitHub 项目读取超时", exception);
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private String proxiedUrl(String targetUrl) {
        URI base;
        try { base = URI.create(readingProxyBaseUrl); }
        catch (IllegalArgumentException exception) { throw new IllegalStateException("GitHub 读取代理地址无效", exception); }
        if (!"https".equalsIgnoreCase(base.getScheme()) || base.getHost() == null || base.getRawUserInfo() != null
                || base.getRawQuery() != null || base.getRawFragment() != null)
            throw new IllegalStateException("GitHub 读取代理必须是无凭据的 HTTPS 地址");
        return (readingProxyBaseUrl.endsWith("/") ? readingProxyBaseUrl : readingProxyBaseUrl + "/") + targetUrl;
    }

    private static int remainingMillis(long deadline) throws IOException {
        long millis = Duration.ofNanos(Math.max(0, deadline - System.nanoTime())).toMillis();
        if (millis <= 0) throw new IOException("GitHub 项目读取超时");
        return (int) Math.min(Integer.MAX_VALUE, millis);
    }

    private static String sourceFingerprint(List<ProjectReadingSource> sources) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (ProjectReadingSource source : sources) {
                digest.update(source.path().getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0);
                digest.update(source.content().getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0);
            }
            return "sha256:" + HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("无法计算项目资料版本", exception);
        }
    }

    private static List<String> readmePaths() {
        return List.of("README.md", "README.en.md", "README.rst", "readme.md");
    }

    private static List<String> manifestPaths(String language) {
        String normalized = language == null ? "" : language.toLowerCase();
        if (normalized.contains("python")) return List.of("pyproject.toml", "requirements.txt", "setup.py");
        if (normalized.contains("java") || normalized.contains("kotlin"))
            return List.of("pom.xml", "build.gradle.kts", "build.gradle");
        if (normalized.contains("javascript") || normalized.contains("typescript")) return List.of("package.json");
        if (normalized.contains("go")) return List.of("go.mod");
        if (normalized.contains("rust")) return List.of("Cargo.toml");
        if (normalized.contains("php")) return List.of("composer.json");
        if (normalized.contains("c#") || normalized.contains("csharp")) return List.of("global.json", "Directory.Build.props");
        if (normalized.equals("c") || normalized.contains("c++") || normalized.contains("cpp"))
            return List.of("CMakeLists.txt", "Makefile");
        return List.of("package.json", "pom.xml", "pyproject.toml", "go.mod", "Cargo.toml");
    }

    private static List<String> documentationPaths() {
        return List.of("docs/ARCHITECTURE.md", "docs/architecture.md", "docs/README.md", "ARCHITECTURE.md");
    }

    private static List<String> entryPointPaths(String language) {
        String normalized = language == null ? "" : language.toLowerCase();
        if (normalized.contains("python")) return List.of("src/main.py", "main.py", "app.py", "src/app.py");
        if (normalized.contains("java") || normalized.contains("kotlin"))
            return List.of("src/main/java/Application.java", "src/main/java/Main.java", "src/main/java/App.java");
        if (normalized.contains("javascript") || normalized.contains("typescript"))
            return List.of("src/index.ts", "src/main.ts", "src/index.js", "src/main.js");
        if (normalized.contains("go")) return List.of("main.go", "cmd/main.go");
        if (normalized.contains("rust")) return List.of("src/main.rs", "src/lib.rs");
        if (normalized.equals("c")) return List.of("src/main.c", "main.c");
        return List.of();
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
        if (parts.length != 2 || !REPO_SEGMENT.matcher(parts[0]).matches() || !REPO_SEGMENT.matcher(parts[1]).matches()
                || ".".equals(parts[0]) || "..".equals(parts[0]) || ".".equals(parts[1]) || "..".equals(parts[1]))
            throw new IllegalArgumentException("无法从来源地址识别 GitHub 仓库");
        String name = parts[1].replaceFirst("\\.git$", "");
        return new RepoPath(parts[0], name);
    }

    private static String encodePathForBrowser(String path) {
        return java.util.Arrays.stream(path.split("/", -1))
                .map(segment -> URLEncoder.encode(segment, StandardCharsets.UTF_8).replace("+", "%20"))
                .collect(Collectors.joining("/"));
    }

    private record RepoPath(String owner, String name) {}
}

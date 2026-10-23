package kd.address.view.service;

import kd.address.view.entity.ExternalProject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Slf4j
@Component
public class GitHubProjectReadingContextReader implements ProjectReadingContextReader {
    private static final Pattern ROOT_MANIFEST = Pattern.compile("(?i)^(pom\\.xml|package\\.json|pyproject\\.toml|go\\.mod|cargo\\.toml|requirements\\.txt|composer\\.json)$");
    private static final int PER_FILE_LIMIT = 9_000;
    private static final int TOTAL_CONTEXT_LIMIT = 36_000;
    private static final int MAX_TREE_OUTPUT_BYTES = 8 * 1024 * 1024;
    private static final int MAX_FILE_OUTPUT_BYTES = 1024 * 1024;
    private static final Duration CLONE_TIMEOUT = Duration.ofSeconds(45);
    private static final Duration COMMAND_TIMEOUT = Duration.ofSeconds(15);

    @Override
    public boolean supports(ExternalProject project) {
        return project != null && "github_search".equals(project.getSourceType());
    }

    @Override
    public ProjectReadingContext read(ExternalProject project) {
        if (!supports(project)) throw new IllegalArgumentException("当前还没有为这个来源配置代码资料读取器");
        RepoPath repo = parseRepo(project.getSourceUrl());
        Path tempRoot = null;
        try {
            tempRoot = Files.createTempDirectory("portal-project-reading-");
            Path checkout = tempRoot.resolve("repo");
            String cloneUrl = "https://github.com/" + repo.owner() + "/" + repo.name() + ".git";
            runGit(tempRoot, CLONE_TIMEOUT, 64 * 1024,
                    "clone", "--quiet", "--filter=blob:none", "--depth=1", "--no-checkout", cloneUrl, checkout.toString());

            String version = runGit(checkout, COMMAND_TIMEOUT, 256, "rev-parse", "HEAD").trim();
            String treeSha = runGit(checkout, COMMAND_TIMEOUT, 256, "rev-parse", "HEAD^{tree}").trim();
            if (version.isBlank() || treeSha.isBlank()) throw new IOException("无法确认仓库当前版本");
            String branch = "default";
            try {
                String remoteHead = runGit(checkout, COMMAND_TIMEOUT, 256,
                        "symbolic-ref", "--short", "refs/remotes/origin/HEAD").trim();
                if (!remoteHead.isBlank()) branch = remoteHead.replaceFirst("^origin/", "");
            } catch (IOException ignored) {
                // Some repositories do not advertise origin/HEAD; the resolved commit is still authoritative.
            }

            List<String> paths = splitNullDelimited(runGitBytes(checkout, COMMAND_TIMEOUT,
                    MAX_TREE_OUTPUT_BYTES, "ls-tree", "-r", "--name-only", "-z", "HEAD"));
            if (paths.isEmpty()) throw new IllegalStateException("仓库文件目录为空或无法读取");

            List<ProjectReadingSource> sources = new ArrayList<>();
            int remaining = TOTAL_CONTEXT_LIMIT;
            for (String path : selectPaths(paths)) {
                if (remaining <= 0) break;
                String content = readBlob(checkout, path, Math.min(PER_FILE_LIMIT, remaining));
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
        } finally {
            deleteTree(tempRoot);
        }
    }

    private static String readBlob(Path repo, String path, int limit) throws IOException, InterruptedException {
        String object = "HEAD:" + path;
        String sizeText = runGit(repo, COMMAND_TIMEOUT, 64, "cat-file", "-s", object).trim();
        long size;
        try { size = Long.parseLong(sizeText); }
        catch (NumberFormatException exception) { throw new IOException("无法读取仓库文件大小：" + path); }
        if (size <= 0 || size > MAX_FILE_OUTPUT_BYTES) return "";
        byte[] body = runGitBytes(repo, COMMAND_TIMEOUT, MAX_FILE_OUTPUT_BYTES, "show", object);
        return new String(body, 0, Math.min(body.length, Math.max(1, limit)), StandardCharsets.UTF_8);
    }

    private static List<String> splitNullDelimited(byte[] bytes) {
        String listing = new String(bytes, StandardCharsets.UTF_8);
        List<String> paths = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < listing.length(); i++) {
            if (listing.charAt(i) == '\0') {
                if (i > start) paths.add(listing.substring(start, i));
                start = i + 1;
            }
        }
        if (start < listing.length()) paths.add(listing.substring(start));
        return paths;
    }

    private static String runGit(Path workingDirectory, Duration timeout, int maxOutputBytes,
                                 String... args) throws IOException, InterruptedException {
        return new String(runGitBytes(workingDirectory, timeout, maxOutputBytes, args), StandardCharsets.UTF_8);
    }

    private static byte[] runGitBytes(Path workingDirectory, Duration timeout, int maxOutputBytes,
                                      String... args) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.add("-C");
        command.add(workingDirectory.toString());
        command.addAll(List.of(args));
        ProcessBuilder builder = new ProcessBuilder(command).redirectErrorStream(true);
        builder.environment().put("GIT_CONFIG_NOSYSTEM", "1");
        builder.environment().put("GIT_CONFIG_GLOBAL", "/dev/null");
        builder.environment().put("GIT_TEMPLATE_DIR", "/dev/null");
        builder.environment().put("GIT_TERMINAL_PROMPT", "0");
        builder.environment().put("GIT_LFS_SKIP_SMUDGE", "1");
        builder.environment().put("GIT_OPTIONAL_LOCKS", "0");
        builder.environment().put("GIT_ALLOW_PROTOCOL", "https");
        builder.environment().put("HOME", workingDirectory.toString());

        Process process = builder.start();
        CompletableFuture<byte[]> output = CompletableFuture.supplyAsync(() -> readBounded(process, maxOutputBytes));
        try {
            if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                output.cancel(true);
                throw new IOException("GitHub 项目读取超时");
            }
        } catch (InterruptedException exception) {
            process.destroyForcibly();
            output.cancel(true);
            Thread.currentThread().interrupt();
            throw exception;
        }

        byte[] bytes;
        try { bytes = output.get(2, TimeUnit.SECONDS); }
        catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof CompletionException completion && completion.getCause() instanceof IOException io)
                throw io;
            throw new IOException("读取 GitHub 命令输出失败", cause);
        } catch (java.util.concurrent.TimeoutException exception) {
            process.destroyForcibly();
            throw new IOException("GitHub 命令输出读取超时", exception);
        }
        if (process.exitValue() != 0) {
            String detail = new String(bytes, StandardCharsets.UTF_8).trim();
            if (detail.length() > 240) detail = detail.substring(0, 240);
            throw new IOException(detail.isBlank() ? "GitHub 项目读取命令失败" : detail);
        }
        return bytes;
    }

    private static byte[] readBounded(Process process, int maxOutputBytes) {
        try (InputStream input = process.getInputStream(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                if (output.size() + read > maxOutputBytes) {
                    process.destroyForcibly();
                    throw new IOException("GitHub 项目读取结果超过大小限制");
                }
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        } catch (IOException exception) {
            throw new CompletionException(exception);
        }
    }

    private static void deleteTree(Path root) {
        if (root == null || !Files.exists(root)) return;
        try (Stream<Path> paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try { Files.deleteIfExists(path); }
                catch (IOException exception) { log.warn("无法清理临时项目资料：{}", path.getFileName()); }
            });
        } catch (IOException exception) {
            log.warn("无法清理临时项目资料目录");
        }
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

    private static String encodePathForBrowser(String path) {
        return java.util.Arrays.stream(path.split("/", -1))
                .map(segment -> URLEncoder.encode(segment, StandardCharsets.UTF_8).replace("+", "%20"))
                .collect(java.util.stream.Collectors.joining("/"));
    }

    private record RepoPath(String owner, String name) {}
}

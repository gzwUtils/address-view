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
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class GitHubRepositoryClient {
    private static final String SEARCH_URL = "https://api.github.com/search/repositories";
    private static final String PLATFORM = "github";
    private static final Set<String> OPEN_SOURCE_LICENSES = Set.of(
            "MIT", "Apache-2.0", "BSD-2-Clause", "BSD-3-Clause", "ISC", "MPL-2.0",
            "GPL-2.0", "GPL-3.0", "GPL-2.0-only", "GPL-3.0-only", "GPL-2.0-or-later",
            "GPL-3.0-or-later", "LGPL-2.1", "LGPL-3.0", "LGPL-2.1-only", "LGPL-3.0-only",
            "AGPL-3.0", "AGPL-3.0-only", "EPL-2.0", "EUPL-1.2", "CC0-1.0", "Unlicense", "Zlib");
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).build();
    private final ObjectMapper json;
    private final String token;

    public GitHubRepositoryClient(ObjectMapper json, @Value("${portal.github.token:}") String token) {
        this.json = json;
        this.token = token;
    }

    public List<ExternalProject> recentLicensedProjects() throws IOException, InterruptedException {
        LocalDate from = LocalDate.now(ZoneOffset.UTC).minusDays(7);
        String query = "created:>=" + from + " stars:>=10 fork:false archived:false is:public";
        String url = SEARCH_URL + "?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8)
                + "&sort=stars&order=desc&per_page=30";
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(12))
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .header("User-Agent", "Zaichang-Portal")
                .GET();
        if (!token.isBlank()) builder.header("Authorization", "Bearer " + token);
        HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("GitHub search returned HTTP " + response.statusCode());
        }
        return parseResults(json.readTree(response.body()));
    }

    List<ExternalProject> parseResults(JsonNode response) {
        List<ExternalProject> selected = new ArrayList<>();
        JsonNode items = response.path("items");
        if (!items.isArray()) return selected;
        for (JsonNode item : items) {
            if (selected.size() >= 5) break;
            String license = item.path("license").path("spdx_id").asText("");
            String fullName = item.path("full_name").asText("");
            String url = item.path("html_url").asText("");
            if (!OPEN_SOURCE_LICENSES.contains(license)
                    || fullName.isBlank() || !url.startsWith("https://github.com/")) continue;
            ExternalProject project = new ExternalProject();
            project.setSourcePlatform(PLATFORM);
            project.setSourceRepoId(item.path("id").asLong());
            project.setFullName(shorten(fullName, 255));
            project.setSourceUrl(shorten(url, 500));
            project.setDescription(shorten(item.path("description").asText(""), 1000));
            project.setLanguage(shorten(item.path("language").asText(""), 80));
            project.setLicenseSpdx(shorten(license, 80));
            project.setStarCount(item.path("stargazers_count").asInt());
            project.setForkCount(item.path("forks_count").asInt());
            try {
                project.setRepoCreatedAt(Instant.parse(item.path("created_at").asText())
                        .atZone(ZoneOffset.UTC).toLocalDateTime());
            } catch (Exception ignored) {
                continue;
            }
            if (project.getSourceRepoId() > 0) selected.add(project);
        }
        return selected;
    }

    private static String shorten(String value, int length) {
        return value.length() > length ? value.substring(0, length) : value;
    }
}

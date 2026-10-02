package kd.address.view.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GitHubRepositoryClientTest {
    private final ObjectMapper json = new ObjectMapper();
    private final GitHubRepositoryClient client = new GitHubRepositoryClient(json, "");

    @Test
    void onlyLicensedGitHubRepositoriesAreSelected() throws Exception {
        var payload = json.readTree("""
                {"items":[
                  {"id":1,"full_name":"example/no-license","html_url":"https://github.com/example/no-license","license":null,"created_at":"2026-09-30T09:00:00Z"},
                  {"id":2,"full_name":"example/unknown","html_url":"https://github.com/example/unknown","license":{"spdx_id":"NOASSERTION"},"created_at":"2026-09-30T09:00:00Z"},
                  {"id":4,"full_name":"example/noncommercial","html_url":"https://github.com/example/noncommercial","license":{"spdx_id":"CC-BY-NC-4.0"},"created_at":"2026-09-30T09:00:00Z"},
                  {"id":3,"full_name":"example/valid","html_url":"https://github.com/example/valid","description":"A useful tool","language":"Java","license":{"spdx_id":"MIT"},"stargazers_count":320,"forks_count":12,"created_at":"2026-09-30T09:00:00Z"}
                ]}
                """);

        var selected = client.parseResults(payload);

        assertThat(selected).hasSize(1);
        assertThat(selected.get(0).getFullName()).isEqualTo("example/valid");
        assertThat(selected.get(0).getLicenseSpdx()).isEqualTo("MIT");
        assertThat(selected.get(0).getStarCount()).isEqualTo(320);
    }
}

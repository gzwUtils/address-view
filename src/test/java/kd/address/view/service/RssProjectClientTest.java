package kd.address.view.service;

import kd.address.view.entity.ExternalSource;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RssProjectClientTest {
    private final RssProjectClient client = new RssProjectClient();

    @Test
    void parsesPublicProjectLinksAndSkipsNonHttpsEntries() throws Exception {
        ExternalSource source = new ExternalSource();
        source.setCode("src-test");
        source.setMaxItems(5);
        String rss = """
                <rss><channel>
                  <item><title>Useful tool</title><link>https://example.org/tool</link>
                    <description>Build things faster</description><pubDate>Fri, 02 Oct 2026 09:00:00 GMT</pubDate></item>
                  <item><title>Unsafe link</title><link>http://localhost/private</link></item>
                </channel></rss>
                """;

        var results = client.parse(rss.getBytes(StandardCharsets.UTF_8), source);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getSourcePlatform()).isEqualTo("src-test");
        assertThat(results.get(0).getSourceUrl()).isEqualTo("https://example.org/tool");
        assertThat(results.get(0).getLicenseSpdx()).isEmpty();
    }

    @Test
    void rejectsXmlExternalEntities() {
        ExternalSource source = new ExternalSource();
        source.setCode("src-test");
        source.setMaxItems(5);
        String rss = "<!DOCTYPE rss [<!ENTITY ex SYSTEM \"file:///etc/passwd\">]><rss><channel>" +
                "<item><title>&ex;</title><link>https://example.org/tool</link></item></channel></rss>";
        assertThatThrownBy(() -> client.parse(rss.getBytes(StandardCharsets.UTF_8), source))
                .isInstanceOf(Exception.class);
    }
}

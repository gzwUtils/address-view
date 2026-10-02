package kd.address.view.service;

import kd.address.view.entity.ExternalSource;
import kd.address.view.mapper.ExternalSourceMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ExternalSourceServiceTest {
    private final ExternalSourceMapper mapper = mock(ExternalSourceMapper.class);
    private final ExternalSourceService service = new ExternalSourceService(mapper);

    @Test
    void adminCanAddAnRssSourceWithoutChangingCode() {
        ExternalSource input = new ExternalSource();
        input.setDisplayName("开源工具周刊");
        input.setSourceType("rss");
        input.setFeedUrl("https://example.org/feed.xml");
        input.setMaxItems(4);
        input.setIntervalHours(24);
        input.setEnabled(true);

        ExternalSource saved = service.save(input);

        assertThat(saved.getCode()).startsWith("src-");
        assertThat(saved.getFeedUrl()).isEqualTo("https://example.org/feed.xml");
        assertThat(saved.getMaxItems()).isEqualTo(4);
        verify(mapper).insert(saved);
    }

    @Test
    void internalOrHttpFeedCannotBeConfigured() {
        ExternalSource input = new ExternalSource();
        input.setDisplayName("内部地址");
        input.setSourceType("rss");
        input.setFeedUrl("http://127.0.0.1/feed");
        input.setMaxItems(5);
        input.setIntervalHours(24);

        assertThatThrownBy(() -> service.save(input)).isInstanceOf(IllegalArgumentException.class);
        input.setFeedUrl("https://127.0.0.1/feed");
        assertThatThrownBy(() -> service.save(input)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(mapper);
    }
}

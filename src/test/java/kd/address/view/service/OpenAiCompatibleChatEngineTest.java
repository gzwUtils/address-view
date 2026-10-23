package kd.address.view.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import kd.address.view.entity.ProjectReadingEngineProfile;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiCompatibleChatEngineTest {
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void extractsTextFromOpenAiContentParts() throws Exception {
        var response = json.readTree("""
                {"choices":[{"message":{"content":[
                  {"type":"text","text":"第一段"},
                  {"type":"text","text":"第二段"}
                ]}}]}
                """);

        assertThat(OpenAiCompatibleChatEngine.responseText(response)).isEqualTo("第一段\n第二段");
    }

    @Test
    void extractsResponsesApiOutputTextFallback() throws Exception {
        var response = json.readTree("{\"output_text\":\"解读正文\"}");

        assertThat(OpenAiCompatibleChatEngine.responseText(response)).isEqualTo("解读正文");
    }

    @Test
    void disablesDeepSeekThinkingForStructuredGuideGeneration() {
        ProjectReadingEngineProfile deepSeek = new ProjectReadingEngineProfile();
        deepSeek.setBaseUrl("https://api.deepseek.com");
        ProjectReadingEngineProfile other = new ProjectReadingEngineProfile();
        other.setBaseUrl("https://example.com/v1");

        assertThat(OpenAiCompatibleChatEngine.shouldDisableDeepSeekThinking(deepSeek)).isTrue();
        assertThat(OpenAiCompatibleChatEngine.shouldDisableDeepSeekThinking(other)).isFalse();
    }
}

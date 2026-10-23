package kd.address.view.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import kd.address.view.entity.ProjectReadingEngineProfile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class OpenAiCompatibleChatEngine implements ProjectReadingEngineAdapter {
    private static final int MAX_RESPONSE_BYTES = 1024 * 1024;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    private final ObjectMapper json;

    public OpenAiCompatibleChatEngine(ObjectMapper json) {
        this.json = json;
    }

    @Override
    public boolean supports(String protocol) {
        return "openai-compatible".equalsIgnoreCase(protocol);
    }

    @Override
    public String complete(ProjectReadingEngineProfile profile, String apiKey,
                           String systemPrompt, String userPrompt) {
        ObjectNode payload = json.createObjectNode();
        payload.put("model", profile.getModelName());
        payload.put("temperature", 0.35);
        payload.put("max_tokens", 2200);
        ArrayNode messages = payload.putArray("messages");
        messages.addObject().put("role", "system").put("content", systemPrompt);
        messages.addObject().put("role", "user").put("content", userPrompt);

        HttpRequest.Builder request = HttpRequest.newBuilder(endpoint(profile.getBaseUrl()))
                .timeout(Duration.ofSeconds(profile.getTimeoutSeconds()))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()));
        if (apiKey != null && !apiKey.isBlank()) request.header("Authorization", "Bearer " + apiKey);

        try {
            HttpResponse<byte[]> response = http.send(request.build(),
                    BoundedBodyHandlers.rejectOversize(MAX_RESPONSE_BYTES));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("解读引擎返回 HTTP " + response.statusCode());
            }
            JsonNode root = json.readTree(response.body());
            String content = root.path("choices").path(0).path("message").path("content").asText("").trim();
            if (content.isBlank()) throw new IllegalStateException("解读引擎没有返回正文");
            return content;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("解读任务已中断", exception);
        } catch (IOException exception) {
            throw new IllegalStateException("无法读取解读引擎响应", exception);
        }
    }

    private static URI endpoint(String baseUrl) {
        String base = baseUrl == null ? "" : baseUrl.trim().replaceAll("/+$", "");
        if (base.endsWith("/chat/completions")) return URI.create(base);
        if (base.endsWith("/v1")) return URI.create(base + "/chat/completions");
        return URI.create(base + "/v1/chat/completions");
    }
}

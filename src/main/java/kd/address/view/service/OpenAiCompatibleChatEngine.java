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
import java.util.StringJoiner;

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
        if (shouldDisableDeepSeekThinking(profile)) payload.putObject("thinking").put("type", "disabled");
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
            String content = responseText(root).trim();
            if (content.isBlank()) throw new IllegalStateException("解读引擎没有返回正文（响应结构：" + responseShape(root) + "）");
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

    static String responseText(JsonNode root) {
        String outputText = textValue(root.path("output_text"));
        if (!outputText.isBlank()) return outputText;
        JsonNode choices = root.path("choices");
        if (!choices.isArray()) return "";
        for (JsonNode choice : choices) {
            String messageText = textValue(choice.path("message").path("content"));
            if (!messageText.isBlank()) return messageText;
            String legacyText = textValue(choice.path("text"));
            if (!legacyText.isBlank()) return legacyText;
        }
        return "";
    }

    static boolean shouldDisableDeepSeekThinking(ProjectReadingEngineProfile profile) {
        if (profile == null || profile.getBaseUrl() == null) return false;
        try {
            return "api.deepseek.com".equalsIgnoreCase(URI.create(profile.getBaseUrl()).getHost());
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static String textValue(JsonNode value) {
        if (value == null || value.isMissingNode() || value.isNull()) return "";
        if (value.isTextual()) return value.asText();
        if (value.isArray()) {
            StringJoiner joined = new StringJoiner("\n");
            value.forEach(part -> {
                String text = part.isTextual() ? part.asText() : part.path("text").asText("");
                if (!text.isBlank()) joined.add(text);
            });
            return joined.toString();
        }
        if (value.isObject()) {
            String text = value.path("text").asText("");
            if (!text.isBlank()) return text;
            String parts = textValue(value.path("parts"));
            if (!parts.isBlank()) return parts;
            if (value.has("projectPurpose") || value.has("readingPath")) return value.toString();
        }
        return "";
    }

    private static String responseShape(JsonNode root) {
        JsonNode choices = root.path("choices");
        JsonNode first = choices.isArray() && !choices.isEmpty() ? choices.get(0) : null;
        JsonNode message = first == null ? null : first.path("message");
        JsonNode content = message == null ? null : message.path("content");
        String contentType = content == null || content.isMissingNode() ? "missing"
                : content.isNull() ? "null" : content.isArray() ? "array"
                : content.isObject() ? "object" : content.isTextual() ? "string" : "other";
        return "root=" + fieldNames(root) + ", choices=" + (choices.isArray() ? choices.size() : 0)
                + ", choice=" + fieldNames(first) + ", message=" + fieldNames(message)
                + ", content=" + contentType + ", finish_reason="
                + (first == null ? "missing" : first.path("finish_reason").asText("missing"));
    }

    private static String fieldNames(JsonNode node) {
        if (node == null || !node.isObject()) return "[]";
        StringJoiner names = new StringJoiner(",", "[", "]");
        node.fieldNames().forEachRemaining(names::add);
        return names.toString();
    }
}

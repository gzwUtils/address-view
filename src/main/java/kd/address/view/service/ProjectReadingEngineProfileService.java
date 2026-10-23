package kd.address.view.service;

import kd.address.view.common.NotFoundException;
import kd.address.view.entity.ProjectReadingEngineProfile;
import kd.address.view.mapper.ProjectReadingEngineProfileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectReadingEngineProfileService {
    private final ProjectReadingEngineProfileMapper profiles;
    private final ProjectReadingSecretCipher cipher;
    private final ProjectReadingEngineRegistry engines;

    public record ProfileInput(Long id, String name, String protocol, String baseUrl, String modelName,
                               String apiKey, Integer timeoutSeconds, Boolean enabled,
                               Boolean defaultForPublic) {}

    public record ProfileView(Long id, String name, String protocol, String baseUrl, String modelName,
                              Integer timeoutSeconds, Boolean enabled, Boolean defaultForPublic,
                              boolean hasApiKey) {}

    public record TestResult(boolean success, String message) {}

    public List<ProfileView> list() {
        return profiles.findAll().stream().map(ProjectReadingEngineProfileService::view).toList();
    }

    @Transactional
    public ProfileView save(ProfileInput input) {
        if (input == null) throw new IllegalArgumentException("请填写解读引擎配置");
        String name = required(input.name(), 100, "引擎名称");
        String protocol = input.protocol() == null || input.protocol().isBlank()
                ? "openai-compatible" : input.protocol().trim().toLowerCase();
        if (!"openai-compatible".equals(protocol)) throw new IllegalArgumentException("当前仅支持 OpenAI 兼容接口");
        String baseUrl = normalizeBaseUrl(input.baseUrl());
        String modelName = required(input.modelName(), 160, "模型名称");
        int timeout = input.timeoutSeconds() == null ? 60 : input.timeoutSeconds();
        if (timeout < 5 || timeout > 180) throw new IllegalArgumentException("超时需在 5–180 秒之间");
        boolean enabled = input.enabled() == null || input.enabled();
        boolean makeDefault = input.defaultForPublic() != null && input.defaultForPublic();
        if (makeDefault && !enabled) throw new IllegalArgumentException("停用的引擎不能设为默认");

        ProjectReadingEngineProfile profile = input.id() == null ? new ProjectReadingEngineProfile() : profiles.findById(input.id());
        if (input.id() != null && profile == null) throw new NotFoundException("解读引擎不存在");
        profile.setName(name);
        profile.setProtocol(protocol);
        profile.setBaseUrl(baseUrl);
        profile.setModelName(modelName);
        profile.setTimeoutSeconds(timeout);
        profile.setEnabled(enabled);
        profile.setDefaultForPublic(makeDefault);
        if (input.apiKey() != null && !input.apiKey().isBlank()) {
            profile.setApiKeyCiphertext(cipher.encrypt(input.apiKey().trim()));
        }

        if (makeDefault) profiles.clearPublicDefault();
        if (profile.getId() == null) profiles.insert(profile);
        else if (profiles.update(profile) == 0) throw new NotFoundException("解读引擎不存在");
        return view(profile);
    }

    public TestResult test(Long id) {
        ProjectReadingEngineProfile profile = requireEnabled(id);
        try {
            String result = engines.complete(profile, cipher.decrypt(profile.getApiKeyCiphertext()),
                    "你正在进行连接测试，只需按要求返回 JSON。",
                    "请只返回 JSON：{\"status\":\"ok\"}。不要添加 Markdown 代码块。上行链路测试。");
            return new TestResult(true, result.length() > 100 ? result.substring(0, 100) : result);
        } catch (RuntimeException exception) {
            return new TestResult(false, safeError(exception));
        }
    }

    public ProjectReadingEngineProfile requireEnabled(Long id) {
        ProjectReadingEngineProfile profile = profiles.findById(id);
        if (profile == null) throw new NotFoundException("解读引擎不存在");
        if (!Boolean.TRUE.equals(profile.getEnabled())) throw new IllegalArgumentException("解读引擎已停用");
        return profile;
    }

    public ProjectReadingEngineProfile defaultForPublic() {
        return profiles.findDefaultForPublic();
    }

    public ProjectReadingEngineProfile findById(Long id) {
        return id == null ? null : profiles.findById(id);
    }

    public String decryptApiKey(ProjectReadingEngineProfile profile) {
        return cipher.decrypt(profile.getApiKeyCiphertext());
    }

    private static ProfileView view(ProjectReadingEngineProfile profile) {
        return new ProfileView(profile.getId(), profile.getName(), profile.getProtocol(), profile.getBaseUrl(),
                profile.getModelName(), profile.getTimeoutSeconds(), profile.getEnabled(),
                profile.getDefaultForPublic(), profile.getApiKeyCiphertext() != null
                && !profile.getApiKeyCiphertext().isBlank());
    }

    private static String required(String value, int max, String label) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isBlank() || normalized.length() > max) throw new IllegalArgumentException(label + "不能为空且长度不能超过 " + max);
        return normalized;
    }

    private static String normalizeBaseUrl(String value) {
        String base = required(value, 500, "接口地址").replaceAll("/+$", "");
        URI uri;
        try { uri = URI.create(base); }
        catch (IllegalArgumentException exception) { throw new IllegalArgumentException("接口地址格式无效"); }
        if (uri.getHost() == null || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null
                || !("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme())))
            throw new IllegalArgumentException("接口地址需使用 HTTP(S) 主机地址，且不能包含账号、查询参数或片段");
        return base;
    }

    private static String safeError(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) return "连接失败，请检查地址、模型和密钥";
        if (message.length() > 180) message = message.substring(0, 180);
        return message;
    }
}

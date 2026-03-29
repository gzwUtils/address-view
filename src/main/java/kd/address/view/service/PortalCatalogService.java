package kd.address.view.service;

import jakarta.annotation.PostConstruct;
import kd.address.view.common.PageResponse;
import kd.address.view.dto.PortalAiCapabilityDTO;
import kd.address.view.dto.PortalAiSceneDTO;
import kd.address.view.dto.PortalContentScopeDTO;
import kd.address.view.dto.PortalMetricDTO;
import kd.address.view.dto.OperationLogDTO;
import kd.address.view.dto.OpsWorkbenchDTO;
import kd.address.view.dto.PortalOverviewDTO;
import kd.address.view.dto.PortalResourceDTO;
import kd.address.view.dto.PortalResourceSaveDTO;
import kd.address.view.dto.RecentViewDTO;
import kd.address.view.dto.RecentViewSaveDTO;
import kd.address.view.dto.PortalSignalCardDTO;
import kd.address.view.dto.TagCountDTO;
import kd.address.view.entity.OperationLog;
import kd.address.view.entity.PortalResource;
import kd.address.view.entity.RecentViewHistory;
import kd.address.view.mapper.OperationLogMapper;
import kd.address.view.mapper.PortalResourceMapper;
import kd.address.view.mapper.RecentViewHistoryMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class PortalCatalogService {

    private static final String KIND_ONLINE = "online";
    private static final String KIND_ARTICLE = "article";
    private static final String KIND_AI = "ai";
    private static final String KIND_LIFE = "life";
    private static final String MODULE_CONTENT = "content";
    private static final String DEFAULT_AI_STATUS = "draft";
    private static final int DEFAULT_RECENT_LIMIT = 8;
    private static final int DEFAULT_PAGE_SIZE = 10;
    static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final PortalResourceMapper portalResourceMapper;
    private final OperationLogMapper operationLogMapper;
    private final RecentViewHistoryMapper recentViewHistoryMapper;

    private volatile PortalOverviewDTO overview;
    private volatile List<PortalResourceDTO> resources;

    public PortalCatalogService(PortalResourceMapper portalResourceMapper,
                                OperationLogMapper operationLogMapper,
                                RecentViewHistoryMapper recentViewHistoryMapper) {
        this.portalResourceMapper = portalResourceMapper;
        this.operationLogMapper = operationLogMapper;
        this.recentViewHistoryMapper = recentViewHistoryMapper;
    }

    @PostConstruct
    public void init() {
        this.overview = buildOverview();
        this.resources = loadResources();
        log.info("PortalCatalogService initialized with {} resources", this.resources.size());
    }

    public PortalOverviewDTO getOverview() {
        return overview;
    }

    public OpsWorkbenchDTO getOpsWorkbench() {
        OpsWorkbenchDTO dto = new OpsWorkbenchDTO();
        List<PortalResourceDTO> current = getResourcesSafe();
        dto.setProjectCount(0);
        dto.setCategoryCount(0);
        dto.setAiCount((int) current.stream().filter(item -> KIND_AI.equals(item.getKind())).count());
        dto.setOnlineAiCount((int) current.stream().filter(item -> KIND_AI.equals(item.getKind()) && KIND_ONLINE.equalsIgnoreCase(item.getStatus())).count());
        dto.setArticleCount((int) current.stream().filter(item -> KIND_ARTICLE.equals(item.getKind())).count());
        dto.setLifeCount((int) current.stream().filter(item -> KIND_LIFE.equals(item.getKind())).count());
        dto.setContentCount(dto.getArticleCount() + dto.getLifeCount());
        dto.setRecentLogs(operationLogMapper.findRecent(DEFAULT_RECENT_LIMIT).stream().map(this::toOperationLogDto).toList());
        return dto;
    }

    public List<RecentViewDTO> getRecentViews(String clientId) {
        if (!StringUtils.hasText(clientId)) {
            return Collections.emptyList();
        }
        return recentViewHistoryMapper.findRecentByClientId(clientId.trim(), DEFAULT_RECENT_LIMIT).stream()
                .map(this::toRecentViewDto)
                .toList();
    }

    public PageResponse<PortalResourceDTO> getResources(String keyword, String type, String tag, Integer page, Integer size) {
        List<PortalResourceDTO> filtered = getResourcesSafe().stream()
                .filter(item -> matchType(item, type))
                .filter(item -> matchTag(item, tag))
                .filter(item -> matchKeyword(item, keyword))
                .toList();
        return buildPage(filtered, page, size);
    }

    public List<TagCountDTO> getTags() {
        Map<String, Integer> counter = new LinkedHashMap<>();
        getResourcesSafe().forEach(item -> {
            if (StringUtils.hasText(item.getCategory())) {
                counter.merge(item.getCategory(), 1, Integer::sum);
            }
            if (StringUtils.hasText(item.getType())) {
                counter.merge(item.getType(), 1, Integer::sum);
            }
            if (item.getTags() != null) {
                item.getTags().forEach(tag -> counter.merge(tag, 1, Integer::sum));
            }
        });

        List<TagCountDTO> result = new ArrayList<>();
        counter.forEach((name, count) -> {
            TagCountDTO dto = new TagCountDTO();
            dto.setName(name);
            dto.setCount(count);
            result.add(dto);
        });
        return result;
    }

    public PageResponse<PortalResourceDTO> getAiAssets(String type, String owner, String status, Integer page, Integer size) {
        List<PortalResourceDTO> filtered = getResourcesSafe().stream()
                .filter(item -> KIND_AI.equals(item.getKind()))
                .filter(item -> !StringUtils.hasText(type) || type.equalsIgnoreCase(item.getType()))
                .filter(item -> !StringUtils.hasText(owner) || owner.equalsIgnoreCase(item.getOwner()))
                .filter(item -> !StringUtils.hasText(status) || status.equalsIgnoreCase(item.getStatus()))
                .toList();
        return buildPage(filtered, page, size);
    }

    @Transactional
    public PortalResourceDTO saveResource(PortalResourceSaveDTO request) {
        PortalResource resource = buildResourceFromRequest(request);

        if (resource.getId() == null) {
            portalResourceMapper.insert(resource);
            writeLog("create", resource.getKind(), resource.getName(), resource.getTitle(), "新增统一资源");
        } else {
            portalResourceMapper.update(resource);
            writeLog("update", resource.getKind(), resource.getName(), resource.getTitle(), "更新统一资源");
        }

        refreshResources();
        PortalResource saved = portalResourceMapper.findById(resource.getId());
        return saved == null ? toDto(resource) : toDto(saved);
    }

    @Transactional
    public void deleteResource(Long id) {
        PortalResource current = portalResourceMapper.findById(id);
        portalResourceMapper.softDeleteById(id);
        if (current != null) {
            writeLog("delete", current.getKind(), current.getName(), current.getTitle(), "删除统一资源");
        }
        refreshResources();
    }

    @Transactional
    public PortalResourceDTO saveAiAsset(PortalResourceSaveDTO request) {
        request.setKind(KIND_AI);
        return saveResource(request);
    }

    @Transactional
    public void deleteAiAsset(Long id) {
        deleteResource(id);
    }

    @Transactional
    public RecentViewDTO saveRecentView(RecentViewSaveDTO request) {
        if (!StringUtils.hasText(request.getClientId())
                || !StringUtils.hasText(request.getKind())
                || request.getTargetId() == null
                || !StringUtils.hasText(request.getTitle())) {
            throw new IllegalArgumentException("clientId, kind, targetId and title are required");
        }

        String clientId = request.getClientId().trim();
        String kind = request.getKind().trim();
        RecentViewHistory existing = recentViewHistoryMapper.findByClientAndTarget(clientId, kind, request.getTargetId());
        if (existing == null) {
            RecentViewHistory history = new RecentViewHistory();
            history.setClientId(clientId);
            history.setKind(kind);
            history.setTargetId(request.getTargetId());
            history.setTitle(request.getTitle().trim());
            history.setSubtitle(StringUtils.hasText(request.getSubtitle()) ? request.getSubtitle().trim() : null);
            recentViewHistoryMapper.insert(history);
            history.setViewTime(LocalDateTime.now());
            return toRecentViewDto(history);
        }

        existing.setTitle(request.getTitle().trim());
        existing.setSubtitle(StringUtils.hasText(request.getSubtitle()) ? request.getSubtitle().trim() : null);
        recentViewHistoryMapper.touch(existing);
        existing.setViewTime(LocalDateTime.now());
        return toRecentViewDto(existing);
    }

    private List<PortalResourceDTO> getResourcesSafe() {
        List<PortalResourceDTO> snapshot = resources;
        return snapshot == null ? Collections.emptyList() : snapshot;
    }

    private PortalResource buildResourceFromRequest(PortalResourceSaveDTO request) {
        PortalResource resource = new PortalResource();
        resource.setId(request.getId());
        resource.setResourceCode(buildResourceCode(request));
        resource.setKind(defaultKind(request.getKind()));
        resource.setTitle(request.getTitle());
        resource.setName(request.getName());
        resource.setExcerpt(request.getExcerpt());
        resource.setDescription(request.getDesc());
        resource.setCategory(request.getCategory());
        resource.setType(request.getType());
        resource.setOwner(request.getOwner());
        resource.setAuthor(request.getAuthor());
        resource.setMeta(request.getMeta());
        resource.setResourceDate(request.getDate());
        resource.setStatus(defaultStatus(request.getKind(), request.getStatus()));
        resource.setVersion(request.getVersion());
        resource.setUpdatedAt(StringUtils.hasText(request.getUpdatedAt()) ? request.getUpdatedAt() : request.getDate());
        resource.setEntryUrl(request.getEntryUrl());
        resource.setCoverImage(request.getCoverImage());
        resource.setContentBody(request.getContentBody());
        resource.setTags(joinCsv(request.getTags()));
        resource.setCapabilities(joinCsv(request.getCapabilities()));
        resource.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        resource.setDeleted(0);
        return resource;
    }

    private List<PortalResourceDTO> loadResources() {
        List<PortalResource> databaseResources = portalResourceMapper.findAllActive();
        if (databaseResources == null || databaseResources.isEmpty()) {
            return buildDefaultResources();
        }
        return databaseResources.stream().map(this::toDto).toList();
    }

    private boolean matchType(PortalResourceDTO item, String type) {
        return !StringUtils.hasText(type) || type.equalsIgnoreCase(item.getKind());
    }

    private boolean matchTag(PortalResourceDTO item, String tag) {
        if (!StringUtils.hasText(tag)) {
            return true;
        }
        return item.getTags() != null && item.getTags().stream().anyMatch(tag::equalsIgnoreCase);
    }

    private boolean matchKeyword(PortalResourceDTO item, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String raw = Stream.of(
                        item.getTitle(),
                        item.getName(),
                        item.getExcerpt(),
                        item.getDesc(),
                        item.getCategory(),
                        item.getType(),
                        item.getOwner(),
                        item.getMeta(),
                        item.getDate(),
                        item.getStatus(),
                        item.getVersion(),
                        item.getUpdatedAt(),
                        item.getEntryUrl(),
                        item.getTags() == null ? "" : String.join(" ", item.getTags()),
                        item.getCapabilities() == null ? "" : String.join(" ", item.getCapabilities())
                )
                .filter(StringUtils::hasText)
                .collect(Collectors.joining(" "))
                .toLowerCase();
        return raw.contains(keyword.toLowerCase());
    }

    private PageResponse<PortalResourceDTO> buildPage(List<PortalResourceDTO> filtered, Integer page, Integer size) {
        int pageNo = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size < 1 ? DEFAULT_PAGE_SIZE : size;
        int fromIndex = Math.min((pageNo - 1) * pageSize, filtered.size());
        int toIndex = Math.min(fromIndex + pageSize, filtered.size());
        return PageResponse.of(filtered.subList(fromIndex, toIndex), filtered.size(), pageNo, pageSize);
    }

    private PortalOverviewDTO buildOverview() {
        PortalOverviewDTO dto = new PortalOverviewDTO();
        dto.setMetrics(List.of(
                metric("4", "内容域"),
                metric("6", "AI 模块建议"),
                metric("24h", "门户可视状态")
        ));
        dto.setTodayFocus(List.of(
                "统一项目与知识检索入口",
                "支持 AI Agent / Skill / MCP 资产目录",
                "在门户视觉中加入科技感与生活感的平衡"
        ));
        dto.setSignalCards(List.of(
                signal("Projects", "服务入口聚合", "保留原项目地址聚合能力，并提升浏览体验。"),
                signal("Knowledge", "笔记文章标签化", "扩展技术文章、复盘、经验卡片与日常记录。"),
                signal("AI Hub", "AI 资产中心", "为智能体、Skill、MCP、OpenClaw 建立统一展示区。"),
                signal("Life", "团队生活流", "公告、灵感和轻内容增强门户的人味。")
        ));
        dto.setContentScopes(List.of(
                contentScope("01", "项目导航", "保留按分类访问系统、平台、工具的现有主链路。"),
                contentScope("02", "笔记文章", "技术分享、故障记录、项目复盘、最佳实践统一管理。"),
                contentScope("03", "AI 资产", "Agent、Prompt、Skill、MCP 连接器、工作流模板。"),
                contentScope("04", "生活空间", "公告、团队日历、城市天气、灵感短句、活动记录。")
        ));
        dto.setAiCapabilities(List.of(
                aiCapability("Agent", "智能助手目录", "收录运维助手、研发 Copilot、文档助手、发布助手，并展示可用能力与入口。",
                        List.of("角色编排", "入口卡片", "权限分层")),
                aiCapability("Skill", "Skill 能力市场", "把常用技能包做成标签化资源，支持适用场景、版本、维护人展示。",
                        List.of("可检索", "版本提示", "团队共享")),
                aiCapability("MCP", "MCP 服务面板", "展示已接入的工具服务、连接状态、可访问资源和推荐调用方式。",
                        List.of("服务状态", "资源说明", "接入指引")),
                aiCapability("Workflow", "OpenClaw 编排建议", "为代码审查、日报生成、故障排查等流程预置工作流模板。",
                        List.of("模板中心", "可复制流程", "协作复用")),
                aiCapability("Search", "智能检索与问答", "在门户里按项目、文章、标签、AI 资产做统一检索，并预留问答入口。",
                        List.of("统一搜索", "知识问答", "相关推荐")),
                aiCapability("Insight", "使用趋势洞察", "统计热门项目、热门文章、常用智能体和资源调用热度。",
                        List.of("热度排行", "活跃趋势", "内容优化"))
        ));
        dto.setAiScenes(List.of(
                aiScene("故障应急", "通过 Agent + MCP 快速读取监控、日志和知识库建议。"),
                aiScene("新人 onboarding", "入口导航、岗位知识、常用 Skill、流程模板集中推荐。"),
                aiScene("需求评审", "调用 AI 助手辅助整理上下文、接口影响面和测试要点。"),
                aiScene("周报复盘", "将项目动态、文章沉淀、团队事件汇总为门户周报。")
        ));
        dto.setWarmPills(List.of("今日纪念日", "城市天气", "午餐推荐", "值班提醒", "团队短文", "活动照片"));
        return dto;
    }

    private List<PortalResourceDTO> buildDefaultResources() {
        List<PortalResourceDTO> list = new ArrayList<>();
        list.add(article(1L, "订单中心迁移复盘", "记录从单体迁移到分层服务后的流量切换、灰度策略与故障回收方式。", "项目复盘",
                "2026-03-22", List.of("订单", "迁移", "灰度")));
        list.add(article(2L, "Spring Boot 接口幂等设计笔记", "梳理令牌、去重表和消息补偿三种常见幂等实现方式及适用边界。", "最佳实践",
                "2026-03-18", List.of("Java", "Spring Boot", "幂等")));
        list.add(article(3L, "一次数据库热点行争用排查", "从监控、慢 SQL、线程栈和业务重试策略四个维度拆解问题。", "故障记录",
                "2026-03-11", List.of("MySQL", "排障", "性能")));
        list.add(aiAsset(11L, "Agent", "研发发布助手", "DevOps Team", KIND_ONLINE, "v2.3.1", "2026-03-26",
                new AiAssetDetail("https://ai.example.com/release-assistant", "用于生成发布检查单、回滚建议和变更摘要。",
                        List.of("发布", "变更", "自动摘要"), List.of("发布摘要", "回滚检查", "风险提示"))));
        list.add(aiAsset(12L, "Skill", "接口设计 Skill 包", "Architecture Group", KIND_ONLINE, "v1.8.0", "2026-03-21",
                new AiAssetDetail("https://ai.example.com/skills/api-design", "面向 RESTful 接口评审与字段规范的团队标准技能集。",
                        List.of("API", "评审", "标准化"), List.of("字段评审", "接口规范", "错误码建议"))));
        list.add(aiAsset(13L, "MCP", "日志检索连接器", "SRE Team", "trial", "v0.9.4", "2026-03-18",
                new AiAssetDetail("https://ai.example.com/mcp/logs", "聚合日志、监控与知识库资源，为故障分析 Agent 提供上下文。",
                        List.of("日志", "监控", "上下文"), List.of("日志聚合", "监控读取", "知识库检索"))));
        list.add(aiAsset(14L, "OpenClaw", "周报编排流", "PMO", "draft", "v0.6.2", "2026-03-15",
                new AiAssetDetail("https://ai.example.com/workflows/weekly-report", "自动拉取项目动态、合并知识沉淀和公告，生成周报初稿。",
                        List.of("编排", "周报", "自动化"), List.of("信息汇总", "周报生成", "流程编排"))));
        list.add(lifeFeed(21L, "公告", "本周五 18:30 团队夜跑", "下班后从园区北门集合，欢迎研发、产品、测试一起参加。", "生活社群"));
        list.add(lifeFeed(22L, "提醒", "清明节前发布窗口收紧", "核心系统发布需提前一天完成风险评估与值班确认。", "运维规范"));
        list.add(lifeFeed(23L, "灵感", "把门户做成团队数字杂志", "除了效率，门户也应该记录团队故事、实践和真实的日常节奏。", "设计建议"));
        return list;
    }

    private PortalResourceDTO toDto(PortalResource entity) {
        PortalResourceDTO dto = new PortalResourceDTO();
        dto.setId(entity.getId());
        dto.setKind(entity.getKind());
        dto.setTitle(entity.getTitle());
        dto.setName(entity.getName());
        dto.setExcerpt(entity.getExcerpt());
        dto.setDesc(entity.getDescription());
        dto.setCategory(entity.getCategory());
        dto.setType(entity.getType());
        dto.setOwner(entity.getOwner());
        dto.setAuthor(entity.getAuthor());
        dto.setMeta(entity.getMeta());
        dto.setDate(entity.getResourceDate());
        dto.setStatus(entity.getStatus());
        dto.setVersion(entity.getVersion());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setEntryUrl(entity.getEntryUrl());
        dto.setCoverImage(entity.getCoverImage());
        dto.setContentBody(entity.getContentBody());
        dto.setTags(splitCsv(entity.getTags()));
        dto.setCapabilities(splitCsv(entity.getCapabilities()));
        return dto;
    }

    private List<String> splitCsv(String value) {
        if (!StringUtils.hasText(value)) {
            return new ArrayList<>();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    private String joinCsv(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.joining(","));
    }

    private String buildResourceCode(PortalResourceSaveDTO request) {
        if (StringUtils.hasText(request.getResourceCode())) {
            return request.getResourceCode().trim();
        }

        String base = Stream.of(request.getKind(), request.getType(), request.getName(), request.getTitle())
                .filter(StringUtils::hasText)
                .collect(Collectors.joining("-"))
                .toLowerCase()
                .replaceAll("[^a-z0-9\\u4e00-\\u9fa5-]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-|-$", "");

        if (!StringUtils.hasText(base)) {
            base = "resource";
        }

        if (request.getId() != null) {
            return base + "-" + request.getId();
        }
        return base + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String defaultKind(String kind) {
        return StringUtils.hasText(kind) ? kind : KIND_ARTICLE;
    }

    private String defaultStatus(String kind, String status) {
        if (StringUtils.hasText(status)) {
            return status;
        }
        if (KIND_AI.equalsIgnoreCase(kind)) {
            return DEFAULT_AI_STATUS;
        }
        return null;
    }

    private void refreshResources() {
        this.resources = loadResources();
    }

    private void writeLog(String action, String targetType, String name, String title, String detail) {
        OperationLog operationLog = new OperationLog();
        operationLog.setModule(MODULE_CONTENT);
        operationLog.setAction(action);
        operationLog.setTargetType(targetType);
        operationLog.setTargetName(StringUtils.hasText(name) ? name : title);
        operationLog.setOperatorName("system");
        operationLog.setDetail(detail);
        operationLogMapper.insert(operationLog);
    }

    private OperationLogDTO toOperationLogDto(OperationLog operationLog) {
        OperationLogDTO dto = new OperationLogDTO();
        dto.setId(operationLog.getId());
        dto.setModule(operationLog.getModule());
        dto.setAction(operationLog.getAction());
        dto.setTargetType(operationLog.getTargetType());
        dto.setTargetName(operationLog.getTargetName());
        dto.setOperatorName(operationLog.getOperatorName());
        dto.setDetail(operationLog.getDetail());
        dto.setCreateTime(operationLog.getCreateTime() == null ? null : operationLog.getCreateTime().format(DATE_TIME_FORMATTER));
        return dto;
    }

    private RecentViewDTO toRecentViewDto(RecentViewHistory history) {
        RecentViewDTO dto = new RecentViewDTO();
        dto.setId(history.getId());
        dto.setClientId(history.getClientId());
        dto.setKind(history.getKind());
        dto.setTargetId(history.getTargetId());
        dto.setTitle(history.getTitle());
        dto.setSubtitle(history.getSubtitle());
        dto.setViewTime(history.getViewTime() == null ? null : history.getViewTime().format(DATE_TIME_FORMATTER));
        return dto;
    }

    private PortalMetricDTO metric(String value, String label) {
        PortalMetricDTO dto = new PortalMetricDTO();
        dto.setValue(value);
        dto.setLabel(label);
        return dto;
    }

    private PortalSignalCardDTO signal(String kicker, String title, String desc) {
        PortalSignalCardDTO dto = new PortalSignalCardDTO();
        dto.setKicker(kicker);
        dto.setTitle(title);
        dto.setDesc(desc);
        return dto;
    }

    private PortalContentScopeDTO contentScope(String icon, String title, String desc) {
        PortalContentScopeDTO dto = new PortalContentScopeDTO();
        dto.setIcon(icon);
        dto.setTitle(title);
        dto.setDesc(desc);
        return dto;
    }

    private PortalAiCapabilityDTO aiCapability(String type, String title, String desc, List<String> tags) {
        PortalAiCapabilityDTO dto = new PortalAiCapabilityDTO();
        dto.setType(type);
        dto.setTitle(title);
        dto.setDesc(desc);
        dto.setTags(tags);
        return dto;
    }

    private PortalAiSceneDTO aiScene(String name, String desc) {
        PortalAiSceneDTO dto = new PortalAiSceneDTO();
        dto.setName(name);
        dto.setDesc(desc);
        return dto;
    }

    private PortalResourceDTO article(Long id, String title, String excerpt, String category, String date, List<String> tags) {
        PortalResourceDTO dto = new PortalResourceDTO();
        dto.setId(id);
        dto.setKind(KIND_ARTICLE);
        dto.setTitle(title);
        dto.setExcerpt(excerpt);
        dto.setCategory(category);
        dto.setDate(date);
        dto.setTags(tags);
        return dto;
    }

    record AiAssetDetail(String entryUrl, String desc, List<String> tags, List<String> capabilities) {}

    private PortalResourceDTO aiAsset(Long id, String type, String name, String owner, String status, String version,
                                      String updatedAt, AiAssetDetail detail) {
        PortalResourceDTO dto = new PortalResourceDTO();
        dto.setId(id);
        dto.setKind(KIND_AI);
        dto.setType(type);
        dto.setName(name);
        dto.setOwner(owner);
        dto.setStatus(status);
        dto.setVersion(version);
        dto.setUpdatedAt(updatedAt);
        dto.setEntryUrl(detail.entryUrl());
        dto.setDesc(detail.desc());
        dto.setTags(detail.tags());
        dto.setCapabilities(detail.capabilities());
        return dto;
    }

    private PortalResourceDTO lifeFeed(Long id, String type, String title, String desc, String meta) {
        PortalResourceDTO dto = new PortalResourceDTO();
        dto.setId(id);
        dto.setKind(KIND_LIFE);
        dto.setType(type);
        dto.setTitle(title);
        dto.setDesc(desc);
        dto.setMeta(meta);
        return dto;
    }
}
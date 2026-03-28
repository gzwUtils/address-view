package kd.address.view.service;

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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class PortalCatalogService {

    private final PortalResourceMapper portalResourceMapper;
    private final OperationLogMapper operationLogMapper;
    private final RecentViewHistoryMapper recentViewHistoryMapper;
    private PortalOverviewDTO overview;
    private List<PortalResourceDTO> resources;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @PostConstruct
    public void init() {
        this.overview = buildOverview();
        this.resources = loadResources();
    }

    public PortalOverviewDTO getOverview() {
        return overview;
    }

    public OpsWorkbenchDTO getOpsWorkbench() {
        OpsWorkbenchDTO dto = new OpsWorkbenchDTO();
        List<PortalResourceDTO> current = resources == null ? new ArrayList<>() : resources;
        dto.setProjectCount(0);
        dto.setCategoryCount(0);
        dto.setAiCount((int) current.stream().filter(item -> "ai".equals(item.getKind())).count());
        dto.setOnlineAiCount((int) current.stream().filter(item -> "ai".equals(item.getKind()) && "online".equalsIgnoreCase(item.getStatus())).count());
        dto.setArticleCount((int) current.stream().filter(item -> "article".equals(item.getKind())).count());
        dto.setLifeCount((int) current.stream().filter(item -> "life".equals(item.getKind())).count());
        dto.setContentCount(dto.getArticleCount() + dto.getLifeCount());
        dto.setRecentLogs(operationLogMapper.findRecent(8).stream().map(this::toOperationLogDto).collect(Collectors.toList()));
        return dto;
    }

    public List<RecentViewDTO> getRecentViews(String clientId) {
        if (!StringUtils.hasText(clientId)) {
            return new ArrayList<>();
        }
        return recentViewHistoryMapper.findRecentByClientId(clientId.trim(), 8).stream()
                .map(this::toRecentViewDto)
                .collect(Collectors.toList());
    }

    public PageResponse<PortalResourceDTO> getResources(String keyword, String type, String tag, Integer page, Integer size) {
        List<PortalResourceDTO> filtered = resources.stream()
                .filter(item -> matchType(item, type))
                .filter(item -> matchTag(item, tag))
                .filter(item -> matchKeyword(item, keyword))
                .collect(Collectors.toList());
        return buildPage(filtered, page, size);
    }

    public List<TagCountDTO> getTags() {
        Map<String, Integer> counter = new LinkedHashMap<>();
        resources.forEach(item -> {
            if (StringUtils.hasText(item.getCategory())) {
                counter.put(item.getCategory(), counter.getOrDefault(item.getCategory(), 0) + 1);
            }
            if (StringUtils.hasText(item.getType())) {
                counter.put(item.getType(), counter.getOrDefault(item.getType(), 0) + 1);
            }
            if (item.getTags() != null) {
                item.getTags().forEach(tag -> counter.put(tag, counter.getOrDefault(tag, 0) + 1));
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
        List<PortalResourceDTO> filtered = resources.stream()
                .filter(item -> "ai".equals(item.getKind()))
                .filter(item -> !StringUtils.hasText(type) || type.equalsIgnoreCase(item.getType()))
                .filter(item -> !StringUtils.hasText(owner) || owner.equalsIgnoreCase(item.getOwner()))
                .filter(item -> !StringUtils.hasText(status) || status.equalsIgnoreCase(item.getStatus()))
                .collect(Collectors.toList());
        return buildPage(filtered, page, size);
    }

    @Transactional
    public PortalResourceDTO saveResource(PortalResourceSaveDTO request) {
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

        if (resource.getId() == null) {
            portalResourceMapper.insert(resource);
            writeLog("content", "create", resource.getKind(), resource.getName(), resource.getTitle(), "新增统一资源");
        } else {
            portalResourceMapper.update(resource);
            writeLog("content", "update", resource.getKind(), resource.getName(), resource.getTitle(), "更新统一资源");
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
            writeLog("content", "delete", current.getKind(), current.getName(), current.getTitle(), "删除统一资源");
        }
        refreshResources();
    }

    @Transactional
    public PortalResourceDTO saveAiAsset(PortalResourceSaveDTO request) {
        request.setKind("ai");
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
            return null;
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

    private List<PortalResourceDTO> loadResources() {
        List<PortalResource> databaseResources = portalResourceMapper.findAllActive();
        if (databaseResources == null || databaseResources.isEmpty()) {
            return buildDefaultResources();
        }
        return databaseResources.stream().map(this::toDto).collect(Collectors.toList());
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
        String raw = Arrays.asList(
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
                ).stream()
                .filter(StringUtils::hasText)
                .collect(Collectors.joining(" "))
                .toLowerCase();
        return raw.contains(keyword.toLowerCase());
    }

    private PageResponse<PortalResourceDTO> buildPage(List<PortalResourceDTO> filtered, Integer page, Integer size) {
        int pageNo = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size < 1 ? 10 : size;
        int fromIndex = Math.min((pageNo - 1) * pageSize, filtered.size());
        int toIndex = Math.min(fromIndex + pageSize, filtered.size());
        return PageResponse.of(filtered.subList(fromIndex, toIndex), filtered.size(), pageNo, pageSize);
    }

    private PortalOverviewDTO buildOverview() {
        PortalOverviewDTO dto = new PortalOverviewDTO();
        dto.setMetrics(Arrays.asList(
                metric("4", "内容域"),
                metric("6", "AI 模块建议"),
                metric("24h", "门户可视状态")
        ));
        dto.setTodayFocus(Arrays.asList(
                "统一项目与知识检索入口",
                "支持 AI Agent / Skill / MCP 资产目录",
                "在门户视觉中加入科技感与生活感的平衡"
        ));
        dto.setSignalCards(Arrays.asList(
                signal("Projects", "服务入口聚合", "保留原项目地址聚合能力，并提升浏览体验。"),
                signal("Knowledge", "笔记文章标签化", "扩展技术文章、复盘、经验卡片与日常记录。"),
                signal("AI Hub", "AI 资产中心", "为智能体、Skill、MCP、OpenClaw 建立统一展示区。"),
                signal("Life", "团队生活流", "公告、灵感和轻内容增强门户的人味。")
        ));
        dto.setContentScopes(Arrays.asList(
                contentScope("01", "项目导航", "保留按分类访问系统、平台、工具的现有主链路。"),
                contentScope("02", "笔记文章", "技术分享、故障记录、项目复盘、最佳实践统一管理。"),
                contentScope("03", "AI 资产", "Agent、Prompt、Skill、MCP 连接器、工作流模板。"),
                contentScope("04", "生活空间", "公告、团队日历、城市天气、灵感短句、活动记录。")
        ));
        dto.setAiCapabilities(Arrays.asList(
                aiCapability("Agent", "智能助手目录", "收录运维助手、研发 Copilot、文档助手、发布助手，并展示可用能力与入口。",
                        Arrays.asList("角色编排", "入口卡片", "权限分层")),
                aiCapability("Skill", "Skill 能力市场", "把常用技能包做成标签化资源，支持适用场景、版本、维护人展示。",
                        Arrays.asList("可检索", "版本提示", "团队共享")),
                aiCapability("MCP", "MCP 服务面板", "展示已接入的工具服务、连接状态、可访问资源和推荐调用方式。",
                        Arrays.asList("服务状态", "资源说明", "接入指引")),
                aiCapability("Workflow", "OpenClaw 编排建议", "为代码审查、日报生成、故障排查等流程预置工作流模板。",
                        Arrays.asList("模板中心", "可复制流程", "协作复用")),
                aiCapability("Search", "智能检索与问答", "在门户里按项目、文章、标签、AI 资产做统一检索，并预留问答入口。",
                        Arrays.asList("统一搜索", "知识问答", "相关推荐")),
                aiCapability("Insight", "使用趋势洞察", "统计热门项目、热门文章、常用智能体和资源调用热度。",
                        Arrays.asList("热度排行", "活跃趋势", "内容优化"))
        ));
        dto.setAiScenes(Arrays.asList(
                aiScene("故障应急", "通过 Agent + MCP 快速读取监控、日志和知识库建议。"),
                aiScene("新人 onboarding", "入口导航、岗位知识、常用 Skill、流程模板集中推荐。"),
                aiScene("需求评审", "调用 AI 助手辅助整理上下文、接口影响面和测试要点。"),
                aiScene("周报复盘", "将项目动态、文章沉淀、团队事件汇总为门户周报。")
        ));
        dto.setWarmPills(Arrays.asList("今日纪念日", "城市天气", "午餐推荐", "值班提醒", "团队短文", "活动照片"));
        return dto;
    }

    private List<PortalResourceDTO> buildDefaultResources() {
        List<PortalResourceDTO> list = new ArrayList<>();
        list.add(article(1L, "订单中心迁移复盘", "记录从单体迁移到分层服务后的流量切换、灰度策略与故障回收方式。", "项目复盘",
                "2026-03-22", Arrays.asList("订单", "迁移", "灰度")));
        list.add(article(2L, "Spring Boot 接口幂等设计笔记", "梳理令牌、去重表和消息补偿三种常见幂等实现方式及适用边界。", "最佳实践",
                "2026-03-18", Arrays.asList("Java", "Spring Boot", "幂等")));
        list.add(article(3L, "一次数据库热点行争用排查", "从监控、慢 SQL、线程栈和业务重试策略四个维度拆解问题。", "故障记录",
                "2026-03-11", Arrays.asList("MySQL", "排障", "性能")));
        list.add(aiAsset(11L, "Agent", "研发发布助手", "DevOps Team", "online", "v2.3.1", "2026-03-26",
                "https://ai.example.com/release-assistant", "用于生成发布检查单、回滚建议和变更摘要。",
                Arrays.asList("发布", "变更", "自动摘要"), Arrays.asList("发布摘要", "回滚检查", "风险提示")));
        list.add(aiAsset(12L, "Skill", "接口设计 Skill 包", "Architecture Group", "online", "v1.8.0", "2026-03-21",
                "https://ai.example.com/skills/api-design", "面向 RESTful 接口评审与字段规范的团队标准技能集。",
                Arrays.asList("API", "评审", "标准化"), Arrays.asList("字段评审", "接口规范", "错误码建议")));
        list.add(aiAsset(13L, "MCP", "日志检索连接器", "SRE Team", "trial", "v0.9.4", "2026-03-18",
                "https://ai.example.com/mcp/logs", "聚合日志、监控与知识库资源，为故障分析 Agent 提供上下文。",
                Arrays.asList("日志", "监控", "上下文"), Arrays.asList("日志聚合", "监控读取", "知识库检索")));
        list.add(aiAsset(14L, "OpenClaw", "周报编排流", "PMO", "draft", "v0.6.2", "2026-03-15",
                "https://ai.example.com/workflows/weekly-report", "自动拉取项目动态、合并知识沉淀和公告，生成周报初稿。",
                Arrays.asList("编排", "周报", "自动化"), Arrays.asList("信息汇总", "周报生成", "流程编排")));
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
                .collect(Collectors.toList());
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

        String base = Arrays.asList(request.getKind(), request.getType(), request.getName(), request.getTitle())
                .stream()
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
        return base + "-" + System.currentTimeMillis();
    }

    private String defaultKind(String kind) {
        return StringUtils.hasText(kind) ? kind : "article";
    }

    private String defaultStatus(String kind, String status) {
        if (StringUtils.hasText(status)) {
            return status;
        }
        if ("ai".equalsIgnoreCase(kind)) {
            return "draft";
        }
        return null;
    }

    private void refreshResources() {
        this.resources = loadResources();
    }

    private void writeLog(String module, String action, String targetType, String name, String title, String detail) {
        OperationLog log = new OperationLog();
        log.setModule(module);
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetName(StringUtils.hasText(name) ? name : title);
        log.setOperatorName("system");
        log.setDetail(detail);
        operationLogMapper.insert(log);
    }

    private OperationLogDTO toOperationLogDto(OperationLog log) {
        OperationLogDTO dto = new OperationLogDTO();
        dto.setId(log.getId());
        dto.setModule(log.getModule());
        dto.setAction(log.getAction());
        dto.setTargetType(log.getTargetType());
        dto.setTargetName(log.getTargetName());
        dto.setOperatorName(log.getOperatorName());
        dto.setDetail(log.getDetail());
        dto.setCreateTime(log.getCreateTime() == null ? null : log.getCreateTime().format(DATE_TIME_FORMATTER));
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
        dto.setKind("article");
        dto.setTitle(title);
        dto.setExcerpt(excerpt);
        dto.setCategory(category);
        dto.setDate(date);
        dto.setTags(tags);
        return dto;
    }

    private PortalResourceDTO aiAsset(Long id, String type, String name, String owner, String status, String version,
                                      String updatedAt, String entryUrl, String desc, List<String> tags,
                                      List<String> capabilities) {
        PortalResourceDTO dto = new PortalResourceDTO();
        dto.setId(id);
        dto.setKind("ai");
        dto.setType(type);
        dto.setName(name);
        dto.setOwner(owner);
        dto.setStatus(status);
        dto.setVersion(version);
        dto.setUpdatedAt(updatedAt);
        dto.setEntryUrl(entryUrl);
        dto.setDesc(desc);
        dto.setTags(tags);
        dto.setCapabilities(capabilities);
        return dto;
    }

    private PortalResourceDTO lifeFeed(Long id, String type, String title, String desc, String meta) {
        PortalResourceDTO dto = new PortalResourceDTO();
        dto.setId(id);
        dto.setKind("life");
        dto.setType(type);
        dto.setTitle(title);
        dto.setDesc(desc);
        dto.setMeta(meta);
        return dto;
    }
}

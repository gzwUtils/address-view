package kd.address.view.service;

import kd.address.view.dto.GrowthCapsuleCheckinDTO;
import kd.address.view.dto.GrowthCapsuleCheckinSaveDTO;
import kd.address.view.dto.GrowthCapsuleItemDTO;
import kd.address.view.dto.GrowthCapsuleItemSaveDTO;
import kd.address.view.dto.GrowthCapsuleOverviewDTO;
import kd.address.view.entity.GrowthCapsule;
import kd.address.view.entity.GrowthCapsuleCheckin;
import kd.address.view.entity.GrowthCapsuleItem;
import kd.address.view.mapper.GrowthCapsuleCheckinMapper;
import kd.address.view.mapper.GrowthCapsuleItemMapper;
import kd.address.view.mapper.GrowthCapsuleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class GrowthCapsuleService {

    private static final String STATUS_TODO = "todo";
    private static final String STATUS_IN_PROGRESS = "in_progress";
    private static final String STATUS_DONE = "done";

    private static final String SOURCE_PROJECT = "project";
    private static final String SOURCE_AI = "ai";
    private static final String SOURCE_ARTICLE = "article";
    private static final String SOURCE_LIFE = "life";

    private static final int EXPECTED_MINUTES_PROJECT = 45;
    private static final int EXPECTED_MINUTES_AI = 30;
    private static final int EXPECTED_MINUTES_LIFE = 15;
    private static final int EXPECTED_MINUTES_DEFAULT = 25;
    private static final int MAX_RECENT_CHECKINS = 3;
    private static final String DEFAULT_MOOD = "steady";

    private final GrowthCapsuleMapper growthCapsuleMapper;
    private final GrowthCapsuleItemMapper growthCapsuleItemMapper;
    private final GrowthCapsuleCheckinMapper growthCapsuleCheckinMapper;

    public GrowthCapsuleOverviewDTO getOverview(String userId) {
        GrowthCapsule capsule = getOrCreateCapsule(userId);
        List<GrowthCapsuleItem> items = growthCapsuleItemMapper.findByCapsuleId(capsule.getId());

        GrowthCapsuleOverviewDTO dto = new GrowthCapsuleOverviewDTO();
        dto.setCapsuleId(capsule.getId());
        dto.setUserId(capsule.getUserId());
        dto.setCapsuleName(capsule.getCapsuleName());
        dto.setTagline(capsule.getTagline());
        dto.setAiBrief(capsule.getAiBrief());
        dto.setTotalItems(items.size());
        dto.setTodoItems((int) items.stream().filter(item -> STATUS_TODO.equals(item.getStatus())).count());
        dto.setInProgressItems((int) items.stream().filter(item -> STATUS_IN_PROGRESS.equals(item.getStatus())).count());
        dto.setDoneItems((int) items.stream().filter(item -> STATUS_DONE.equals(item.getStatus())).count());
        dto.setStreakDays(calculateStreakDays(capsule.getId()));
        dto.setTodayCheckins(growthCapsuleCheckinMapper.countTodayByCapsuleId(capsule.getId()));
        dto.setTodaySuggestion(buildTodaySuggestion(items));
        dto.setItems(items.stream().map(this::toItemDto).toList());
        return dto;
    }

    @Transactional
    public GrowthCapsuleItemDTO addItem(GrowthCapsuleItemSaveDTO request) {
        if (!StringUtils.hasText(request.getUserId())
                || !StringUtils.hasText(request.getSourceKind())
                || request.getSourceId() == null
                || !StringUtils.hasText(request.getTitle())) {
            throw new IllegalArgumentException("userId, sourceKind, sourceId and title are required");
        }

        GrowthCapsule capsule = getOrCreateCapsule(request.getUserId());
        GrowthCapsuleItem existing = growthCapsuleItemMapper.findByCapsuleAndSource(capsule.getId(), request.getSourceKind().trim(), request.getSourceId());
        if (existing != null) {
            if (StringUtils.hasText(request.getNote())) {
                existing.setNote(request.getNote().trim());
            }
            growthCapsuleItemMapper.updateProgress(existing);
            GrowthCapsuleItem refreshed = growthCapsuleItemMapper.findById(existing.getId());
            return refreshed == null ? toItemDto(existing) : toItemDto(refreshed);
        }

        GrowthCapsuleItem item = new GrowthCapsuleItem();
        item.setCapsuleId(capsule.getId());
        item.setSourceKind(request.getSourceKind().trim());
        item.setSourceId(request.getSourceId());
        item.setTitle(request.getTitle().trim());
        item.setSubtitle(StringUtils.hasText(request.getSubtitle()) ? request.getSubtitle().trim() : null);
        item.setNote(StringUtils.hasText(request.getNote()) ? request.getNote().trim() : null);
        item.setValueSummary(buildValueSummary(item));
        item.setFirstStep(buildFirstStep(item));
        item.setSevenDayPlan(buildSevenDayPlan());
        item.setActionPlan(buildActionPlan(item));
        item.setStatus(STATUS_TODO);
        item.setExpectedMinutes(resolveExpectedMinutes(item.getSourceKind()));
        growthCapsuleItemMapper.insert(item);
        refreshCapsuleBrief(capsule);
        GrowthCapsuleItem saved = growthCapsuleItemMapper.findById(item.getId());
        return saved == null ? toItemDto(item) : toItemDto(saved);
    }

    @Transactional
    public GrowthCapsuleItemDTO updateStatus(Long itemId, String status, String note) {
        GrowthCapsuleItem item = growthCapsuleItemMapper.findById(itemId);
        if (item == null || !StringUtils.hasText(status)) {
            throw new IllegalArgumentException("Item not found or status is required");
        }
        item.setStatus(status.trim());
        if (note != null) {
            item.setNote(note.trim());
        }
        growthCapsuleItemMapper.updateProgress(item);
        GrowthCapsuleItem refreshed = growthCapsuleItemMapper.findById(itemId);
        return refreshed == null ? toItemDto(item) : toItemDto(refreshed);
    }

    @Transactional
    public GrowthCapsuleCheckinDTO addCheckin(Long itemId, GrowthCapsuleCheckinSaveDTO request) {
        GrowthCapsuleItem item = growthCapsuleItemMapper.findById(itemId);
        if (item == null || request == null || !StringUtils.hasText(request.getContent())) {
            throw new IllegalArgumentException("Item not found or checkin content is required");
        }
        GrowthCapsuleCheckin checkin = new GrowthCapsuleCheckin();
        checkin.setItemId(itemId);
        checkin.setContent(request.getContent().trim());
        checkin.setMood(StringUtils.hasText(request.getMood()) ? request.getMood().trim() : DEFAULT_MOOD);
        growthCapsuleCheckinMapper.insert(checkin);
        if (!STATUS_DONE.equals(item.getStatus())) {
            item.setStatus(STATUS_IN_PROGRESS);
            growthCapsuleItemMapper.updateProgress(item);
        }
        return toCheckinDto(checkin);
    }

    private GrowthCapsule getOrCreateCapsule(String userId) {
        String normalizedUserId = StringUtils.hasText(userId) ? userId.trim() : "visitor";
        GrowthCapsule capsule = growthCapsuleMapper.findByUserId(normalizedUserId);
        if (capsule != null) {
            return capsule;
        }

        GrowthCapsule next = new GrowthCapsule();
        next.setUserId(normalizedUserId);
        next.setCapsuleName("我的灵感落地舱");
        next.setTagline("把看过的内容，变成会发生的改变。");
        next.setAiBrief("从一篇文章、一个项目或一个 AI 资产开始，持续累积你的行动轨迹。");
        growthCapsuleMapper.insert(next);
        log.info("Created new growth capsule for user: {}", normalizedUserId);
        return next;
    }

    private void refreshCapsuleBrief(GrowthCapsule capsule) {
        List<GrowthCapsuleItem> items = growthCapsuleItemMapper.findByCapsuleId(capsule.getId());
        capsule.setAiBrief(buildAiBrief(items));
        growthCapsuleMapper.update(capsule);
    }

    private String buildAiBrief(List<GrowthCapsuleItem> items) {
        if (items == null || items.isEmpty()) {
            return "从一篇文章、一个项目或一个 AI 资产开始，持续累积你的行动轨迹。";
        }
        long aiCount = items.stream().filter(item -> SOURCE_AI.equals(item.getSourceKind())).count();
        long articleCount = items.stream().filter(item -> SOURCE_ARTICLE.equals(item.getSourceKind())).count();
        long projectCount = items.stream().filter(item -> SOURCE_PROJECT.equals(item.getSourceKind())).count();
        long lifeCount = items.stream().filter(item -> SOURCE_LIFE.equals(item.getSourceKind())).count();
        return "你已沉淀 " + items.size() + " 个行动卡，其中项目 " + projectCount + " 个、文章 " + articleCount + " 个、AI 资产 " + aiCount + " 个、生活灵感 " + lifeCount + " 个。";
    }

    private int calculateStreakDays(Long capsuleId) {
        List<LocalDate> dates = growthCapsuleCheckinMapper.findCheckinDatesByCapsuleId(capsuleId);
        if (dates == null || dates.isEmpty()) {
            return 0;
        }

        LocalDate cursor = LocalDate.now();
        int streak = 0;
        for (LocalDate date : dates) {
            if (date.equals(cursor)) {
                streak++;
                cursor = cursor.minusDays(1);
                continue;
            }
            if (streak == 0 && date.equals(cursor.minusDays(1))) {
                streak++;
                cursor = cursor.minusDays(2);
                continue;
            }
            break;
        }
        return streak;
    }

    private String buildTodaySuggestion(List<GrowthCapsuleItem> items) {
        if (items == null || items.isEmpty()) {
            return "今天先挑一条最打动你的内容加入落地舱，别追求多，先形成第一张行动卡。";
        }

        GrowthCapsuleItem inProgress = items.stream()
                .filter(item -> STATUS_IN_PROGRESS.equals(item.getStatus()))
                .findFirst()
                .orElse(null);
        if (inProgress != null) {
            return "今天优先推进「" + inProgress.getTitle() + "」，按第一步建议完成一个最小动作，并补一条打卡记录。";
        }

        GrowthCapsuleItem todo = items.stream()
                .filter(item -> STATUS_TODO.equals(item.getStatus()))
                .findFirst()
                .orElse(items.get(0));
        return "今天适合启动「" + todo.getTitle() + "」，先用 " + resolveExpectedMinutes(todo.getSourceKind()) + " 分钟做一次最小实践。";
    }

    private GrowthCapsuleItemDTO toItemDto(GrowthCapsuleItem item) {
        GrowthCapsuleItemDTO dto = new GrowthCapsuleItemDTO();
        dto.setId(item.getId());
        dto.setSourceKind(item.getSourceKind());
        dto.setSourceId(item.getSourceId());
        dto.setTitle(item.getTitle());
        dto.setSubtitle(item.getSubtitle());
        dto.setNote(item.getNote());
        dto.setActionPlan(item.getActionPlan());
        dto.setValueSummary(item.getValueSummary());
        dto.setFirstStep(item.getFirstStep());
        dto.setSevenDayPlan(item.getSevenDayPlan());
        dto.setStatus(item.getStatus());
        dto.setExpectedMinutes(item.getExpectedMinutes());
        dto.setCreatedAt(item.getCreateTime() == null ? null : item.getCreateTime().format(PortalCatalogService.DATE_TIME_FORMATTER));
        dto.setUpdatedAt(item.getUpdateTime() == null ? null : item.getUpdateTime().format(PortalCatalogService.DATE_TIME_FORMATTER));
        List<GrowthCapsuleCheckinDTO> checkins = growthCapsuleCheckinMapper.findByItemId(item.getId())
                .stream()
                .limit(MAX_RECENT_CHECKINS)
                .map(this::toCheckinDto)
                .toList();
        dto.setCheckinCount(growthCapsuleCheckinMapper.countByItemId(item.getId()));
        dto.setRecentCheckins(checkins);
        return dto;
    }

    private GrowthCapsuleCheckinDTO toCheckinDto(GrowthCapsuleCheckin checkin) {
        GrowthCapsuleCheckinDTO dto = new GrowthCapsuleCheckinDTO();
        dto.setId(checkin.getId());
        dto.setItemId(checkin.getItemId());
        dto.setContent(checkin.getContent());
        dto.setMood(checkin.getMood());
        dto.setCreateTime(checkin.getCreateTime() == null ? null : checkin.getCreateTime().format(PortalCatalogService.DATE_TIME_FORMATTER));
        return dto;
    }

    private Integer resolveExpectedMinutes(String sourceKind) {
        if (SOURCE_PROJECT.equals(sourceKind)) {
            return EXPECTED_MINUTES_PROJECT;
        }
        if (SOURCE_AI.equals(sourceKind)) {
            return EXPECTED_MINUTES_AI;
        }
        if (SOURCE_LIFE.equals(sourceKind)) {
            return EXPECTED_MINUTES_LIFE;
        }
        return EXPECTED_MINUTES_DEFAULT;
    }

    private String buildValueSummary(GrowthCapsuleItem item) {
        List<String> chunks = new ArrayList<>();
        chunks.add("这条内容不是一次性浏览，它适合沉淀成你的长期能力资产。");
        if (StringUtils.hasText(item.getSubtitle())) {
            chunks.add("当前主题聚焦在\u201c" + item.getSubtitle() + "\u201d，适合拿来形成稳定的方法或习惯。");
        }
        return String.join(" ", chunks);
    }

    private String buildFirstStep(GrowthCapsuleItem item) {
        if (SOURCE_PROJECT.equals(item.getSourceKind())) {
            return "先打开项目入口，记录你最想解决的一个真实问题，并写下 1 条试用结论。";
        }
        if (SOURCE_AI.equals(item.getSourceKind())) {
            return "先用这项 AI 能力完成一个 10 分钟以内的小任务，再评估是否值得纳入日常流程。";
        }
        if (SOURCE_LIFE.equals(item.getSourceKind())) {
            return "先把这条灵感改写成一句你愿意今天就执行的话，并安排一个明确时间点。";
        }
        return "先提炼这条内容最有价值的 1 个观点，并把它应用到你当前最具体的一件事情上。";
    }

    private String buildSevenDayPlan() {
        return "Day1 读懂并摘出关键点；Day2 做一次最小实践；Day3 记录问题；Day4 结合你自己的场景调整；Day5 再做一次；Day6 输出一条心得；Day7 判断是否长期保留。";
    }

    private String buildActionPlan(GrowthCapsuleItem item) {
        return "价值定位：" + buildValueSummary(item) + "\n第一步：" + buildFirstStep(item) + "\n7天推进：" + buildSevenDayPlan();
    }
}
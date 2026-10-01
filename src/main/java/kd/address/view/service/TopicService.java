package kd.address.view.service;

import kd.address.view.common.NotFoundException;
import kd.address.view.common.PageResponse;
import kd.address.view.dto.TopicRequest;
import kd.address.view.entity.CommunityBoard;
import kd.address.view.entity.CommunityTopic;
import kd.address.view.mapper.CommunityBoardMapper;
import kd.address.view.mapper.CommunityTopicMapper;
import kd.address.view.mapper.ProjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TopicService {
    private final CommunityBoardMapper boards;
    private final CommunityTopicMapper topics;
    private final ProjectMapper projects;
    private final IdentityRateLimiter limiter;

    public List<CommunityBoard> boards() { return boards.findVisible(); }

    public PageResponse<CommunityTopic> list(String boardCode, Long projectId, String keyword,
                                              String sort, int page, int size, Long accountId) {
        validatePage(page, size);
        if (boardCode != null && !boardCode.isBlank() && boards.findVisibleByCode(boardCode) == null)
            throw new NotFoundException("板块不存在");
        String order = sort == null || sort.isBlank() ? "recent" : sort;
        if (!"recent".equals(order) && !"new".equals(order)) throw new IllegalArgumentException("排序方式无效");
        String search = keyword == null ? "" : keyword.trim();
        if (search.length() > 100) throw new IllegalArgumentException("关键词过长");
        List<CommunityTopic> records = topics.findPage(boardCode, projectId, search, order, (page - 1) * size, size);
        records.forEach(topic -> markOwnership(topic, accountId));
        return PageResponse.of(records, topics.count(boardCode, projectId, search), page, size);
    }

    public PageResponse<CommunityTopic> mine(Long accountId, int page, int size) {
        validatePage(page, size);
        List<CommunityTopic> records = topics.findMine(accountId, (page - 1) * size, size);
        records.forEach(topic -> markOwnership(topic, accountId));
        return PageResponse.of(records, topics.countMine(accountId), page, size);
    }

    public CommunityTopic get(Long id, Long accountId) {
        CommunityTopic topic = topics.findById(id);
        if (topic == null || !"visible".equals(topic.getStatus()) || boards.findVisibleByCode(topic.getBoardCode()) == null)
            throw new NotFoundException("主题不存在");
        markOwnership(topic, accountId);
        return topic;
    }

    @Transactional
    public CommunityTopic create(TopicRequest request, Long accountId, String ip) {
        Validated value = validate(request);
        limiter.checkAction("topic", accountId, ip, 5, 20, false);
        CommunityTopic topic = new CommunityTopic();
        topic.setBoardId(value.board().getId());
        topic.setProjectId(request.projectId());
        topic.setAuthorAccountId(accountId);
        topic.setTitle(value.title());
        topic.setBody(value.body());
        topics.insert(topic);
        return get(topic.getId(), accountId);
    }

    @Transactional
    public CommunityTopic update(Long id, TopicRequest request, Long accountId) {
        CommunityTopic current = get(id, accountId);
        requireAuthor(current, accountId);
        Validated value = validate(request);
        current.setBoardId(value.board().getId());
        current.setProjectId(request.projectId());
        current.setTitle(value.title());
        current.setBody(value.body());
        topics.updateContent(current);
        return get(id, accountId);
    }

    @Transactional
    public void delete(Long id, Long accountId) {
        CommunityTopic current = get(id, accountId);
        requireAuthor(current, accountId);
        topics.softDelete(id);
    }

    private Validated validate(TopicRequest request) {
        if (request == null) throw new IllegalArgumentException("主题内容不能为空");
        CommunityBoard board = boards.findVisibleByCode(request.boardCode());
        if (board == null) throw new IllegalArgumentException("请选择有效板块");
        if (request.projectId() != null && projects.findById(request.projectId()) == null)
            throw new IllegalArgumentException("关联项目不存在");
        String title = request.title() == null ? "" : request.title().trim();
        String body = request.body() == null ? "" : request.body().trim();
        int titleLength = title.codePointCount(0, title.length());
        int bodyLength = body.codePointCount(0, body.length());
        if (titleLength < 5 || titleLength > 120) throw new IllegalArgumentException("标题需要 5–120 字");
        if (bodyLength < 10 || bodyLength > 10000) throw new IllegalArgumentException("正文需要 10–10000 字");
        if (title.codePoints().anyMatch(Character::isISOControl) || body.codePoints().anyMatch(cp -> Character.isISOControl(cp) && cp != '\n' && cp != '\t'))
            throw new IllegalArgumentException("内容包含不可见控制字符");
        return new Validated(board, title, body);
    }

    private static void validatePage(int page, int size) {
        if (page < 1 || size < 1 || size > 50) throw new IllegalArgumentException("分页参数无效");
    }

    private static void markOwnership(CommunityTopic topic, Long accountId) {
        topic.setCanEdit(accountId != null && accountId.equals(topic.getAuthorAccountId()));
    }

    private static void requireAuthor(CommunityTopic topic, Long accountId) {
        if (accountId == null || !accountId.equals(topic.getAuthorAccountId()))
            throw new SecurityException("只能修改自己的主题");
    }

    private record Validated(CommunityBoard board, String title, String body) {}
}

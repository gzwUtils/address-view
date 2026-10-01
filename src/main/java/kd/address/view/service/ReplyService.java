package kd.address.view.service;

import kd.address.view.common.ConflictException;
import kd.address.view.common.NotFoundException;
import kd.address.view.common.PageResponse;
import kd.address.view.dto.ReplyRequest;
import kd.address.view.entity.CommunityReply;
import kd.address.view.entity.CommunityTopic;
import kd.address.view.mapper.CommunityReplyMapper;
import kd.address.view.mapper.CommunityTopicMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReplyService {
    private final CommunityReplyMapper replies;
    private final CommunityTopicMapper topics;
    private final TopicService topicService;
    private final IdentityRateLimiter limiter;

    public PageResponse<CommunityReply> list(Long topicId, int page, int size, Long accountId) {
        topicService.get(topicId, accountId);
        if (page < 1 || size < 1 || size > 50) throw new IllegalArgumentException("分页参数无效");
        List<CommunityReply> records = replies.findPage(topicId, (page - 1) * size, size);
        records.forEach(reply -> markOwnership(reply, accountId));
        return PageResponse.of(records, replies.countAll(topicId), page, size);
    }

    public long countAfter(Long topicId, Long afterId) {
        topicService.get(topicId, null);
        return replies.countAfter(topicId, afterId == null ? 0 : afterId);
    }

    @Transactional
    public CommunityReply create(Long topicId, ReplyRequest request, Long accountId, String ip) {
        String body = validateBody(request == null ? null : request.body());
        limiter.checkAction("reply", accountId, ip, 30, 100, false);
        CommunityTopic topic = topics.lockById(topicId);
        if (topic == null || !"visible".equals(topic.getStatus())) throw new NotFoundException("主题不存在");
        topicService.get(topicId, accountId);
        if (request.replyToId() != null) {
            CommunityReply target = replies.findById(request.replyToId());
            if (target == null || !topicId.equals(target.getTopicId()) || !"visible".equals(target.getStatus()))
                throw new IllegalArgumentException("引用的楼层不存在");
        }
        int floor = topic.getNextFloor();
        replies.insert(topicId, accountId, request.replyToId(), floor, body);
        topics.updateReplyStats(topicId, floor + 1, topic.getReplyCount() + 1, LocalDateTime.now(ZoneOffset.UTC));
        CommunityReply reply = replies.findByTopicAndFloor(topicId, floor);
        markOwnership(reply, accountId);
        return reply;
    }

    @Transactional
    public CommunityReply update(Long replyId, String body, Long accountId) {
        CommunityReply reply = requireVisible(replyId, accountId);
        requireAuthor(reply, accountId);
        replies.updateBody(replyId, validateBody(body));
        CommunityReply updated = replies.findById(replyId);
        markOwnership(updated, accountId);
        return updated;
    }

    @Transactional
    public void delete(Long replyId, Long accountId) {
        CommunityReply reply = requireVisible(replyId, accountId);
        requireAuthor(reply, accountId);
        topics.lockById(reply.getTopicId());
        if (replies.softDelete(replyId) == 0) throw new ConflictException("回复已经删除");
        recalculate(reply.getTopicId());
    }

    @Transactional
    public void hide(Long replyId) {
        CommunityReply reply = replies.findById(replyId);
        if (reply == null || !"visible".equals(reply.getStatus())) throw new NotFoundException("回复不存在");
        topics.lockById(reply.getTopicId());
        replies.hide(replyId);
        recalculate(reply.getTopicId());
    }

    private CommunityReply requireVisible(Long replyId, Long accountId) {
        CommunityReply reply = replies.findById(replyId);
        if (reply == null || !"visible".equals(reply.getStatus())) throw new NotFoundException("回复不存在");
        topicService.get(reply.getTopicId(), accountId);
        return reply;
    }

    private void recalculate(Long topicId) {
        topics.setReplyStats(topicId, replies.countVisible(topicId), replies.latestVisibleTime(topicId));
    }

    private static String validateBody(String value) {
        String body = value == null ? "" : value.trim();
        int length = body.codePointCount(0, body.length());
        if (length < 1 || length > 2000) throw new IllegalArgumentException("回复需要 1–2000 字");
        if (body.codePoints().anyMatch(cp -> Character.isISOControl(cp) && cp != '\n' && cp != '\t'))
            throw new IllegalArgumentException("回复包含不可见控制字符");
        return body;
    }

    private static void requireAuthor(CommunityReply reply, Long accountId) {
        if (accountId == null || !accountId.equals(reply.getAuthorAccountId()))
            throw new SecurityException("只能修改自己的回复");
    }

    private static void markOwnership(CommunityReply reply, Long accountId) {
        reply.setCanEdit("visible".equals(reply.getStatus()) && accountId != null && accountId.equals(reply.getAuthorAccountId()));
        if (!"visible".equals(reply.getStatus())) reply.setBody("[这条回复已删除或隐藏]");
    }
}

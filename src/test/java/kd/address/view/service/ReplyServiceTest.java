package kd.address.view.service;

import kd.address.view.dto.ReplyRequest;
import kd.address.view.entity.CommunityReply;
import kd.address.view.entity.CommunityTopic;
import kd.address.view.mapper.CommunityReplyMapper;
import kd.address.view.mapper.CommunityTopicMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ReplyServiceTest {
    private CommunityReplyMapper replies;
    private CommunityTopicMapper topics;
    private TopicService topicService;
    private ReplyService service;

    @BeforeEach
    void setUp() {
        replies = mock(CommunityReplyMapper.class);
        topics = mock(CommunityTopicMapper.class);
        topicService = mock(TopicService.class);
        service = new ReplyService(replies, topics, topicService, mock(IdentityRateLimiter.class));
    }

    @Test
    void firstReplyGetsFloorTwo() {
        CommunityTopic topic = new CommunityTopic();
        topic.setId(4L);
        topic.setStatus("visible");
        topic.setNextFloor(2);
        topic.setReplyCount(0);
        when(topics.lockById(4L)).thenReturn(topic);
        CommunityReply inserted = new CommunityReply();
        inserted.setTopicId(4L);
        inserted.setFloorNo(2);
        inserted.setStatus("visible");
        when(replies.findByTopicAndFloor(4L, 2)).thenReturn(inserted);

        CommunityReply result = service.create(4L, new ReplyRequest("你好", null), 7L, "127.0.0.1");
        assertEquals(2, result.getFloorNo());
        verify(topics).updateReplyStats(eq(4L), eq(3), eq(1), any());
    }

    @Test
    void onlyAuthorCanDeleteReply() {
        CommunityReply reply = new CommunityReply();
        reply.setId(31L);
        reply.setTopicId(4L);
        reply.setAuthorAccountId(7L);
        reply.setStatus("visible");
        when(replies.findById(31L)).thenReturn(reply);
        assertThrows(SecurityException.class, () -> service.delete(31L, 8L));
        verify(replies, never()).softDelete(31L);
    }
}

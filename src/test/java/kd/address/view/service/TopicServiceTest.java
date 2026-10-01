package kd.address.view.service;

import kd.address.view.dto.TopicRequest;
import kd.address.view.entity.CommunityBoard;
import kd.address.view.entity.CommunityTopic;
import kd.address.view.mapper.CommunityBoardMapper;
import kd.address.view.mapper.CommunityTopicMapper;
import kd.address.view.mapper.ProjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TopicServiceTest {
    private CommunityBoardMapper boards;
    private CommunityTopicMapper topics;
    private TopicService service;

    @BeforeEach
    void setUp() {
        boards = mock(CommunityBoardMapper.class);
        topics = mock(CommunityTopicMapper.class);
        service = new TopicService(boards, topics, mock(ProjectMapper.class), mock(IdentityRateLimiter.class));
        CommunityBoard board = new CommunityBoard();
        board.setId(2L);
        board.setCode("tech-talk");
        when(boards.findVisibleByCode("tech-talk")).thenReturn(board);
    }

    @Test
    void anotherAccountCannotEditTopicEvenWithItsPublicId() {
        CommunityTopic topic = new CommunityTopic();
        topic.setId(14L);
        topic.setAuthorAccountId(22L);
        topic.setBoardCode("tech-talk");
        topic.setStatus("visible");
        when(topics.findById(14L)).thenReturn(topic);

        assertThrows(SecurityException.class, () -> service.update(14L,
                new TopicRequest("tech-talk", null, "新的技术讨论", "这是一段足够长的主题正文。"), 23L));
        verify(topics, never()).updateContent(any());
    }

    @Test
    void rejectsShortTopicBeforeInsert() {
        assertThrows(IllegalArgumentException.class, () -> service.create(
                new TopicRequest("tech-talk", null, "短", "内容不足"), 22L, "127.0.0.1"));
        verify(topics, never()).insert(any());
    }
}

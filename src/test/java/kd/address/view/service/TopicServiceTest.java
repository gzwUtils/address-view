package kd.address.view.service;

import kd.address.view.dto.TopicRequest;
import kd.address.view.entity.CommunityBoard;
import kd.address.view.entity.CommunityTopic;
import kd.address.view.entity.ExternalProject;
import kd.address.view.mapper.CommunityBoardMapper;
import kd.address.view.mapper.CommunityTopicMapper;
import kd.address.view.mapper.CommunityTopicContextMapper;
import kd.address.view.mapper.ExternalProjectMapper;
import kd.address.view.mapper.ProjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TopicServiceTest {
    private CommunityBoardMapper boards;
    private CommunityTopicMapper topics;
    private ExternalProjectMapper externalProjects;
    private CommunityTopicContextMapper contexts;
    private TopicService service;

    @BeforeEach
    void setUp() {
        boards = mock(CommunityBoardMapper.class);
        topics = mock(CommunityTopicMapper.class);
        externalProjects = mock(ExternalProjectMapper.class);
        contexts = mock(CommunityTopicContextMapper.class);
        service = new TopicService(boards, topics, mock(ProjectMapper.class), externalProjects,
                contexts, mock(IdentityRateLimiter.class));
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

    @Test
    void readerReflectionMustBeAttachedToAProject() {
        assertThrows(IllegalArgumentException.class, () -> service.create(
                new TopicRequest("tech-talk", null, null, "reflection", "读后感标题", "这是一段足够长的正文。"),
                22L, "127.0.0.1"));
        verify(topics, never()).insert(any());
    }

    @Test
    void externalProjectReflectionKeepsItsProjectContext() {
        ExternalProject externalProject = new ExternalProject();
        externalProject.setId(21L);
        externalProject.setFullName("example/learning-project");
        when(externalProjects.findById(21L)).thenReturn(externalProject);
        CommunityTopic created = new CommunityTopic();
        created.setId(9L);
        created.setBoardCode("tech-talk");
        created.setStatus("visible");
        doAnswer(invocation -> {
            CommunityTopic inserted = invocation.getArgument(0);
            inserted.setId(9L);
            return null;
        }).when(topics).insert(any());
        when(topics.findById(9L)).thenReturn(created);

        service.create(new TopicRequest("tech-talk", null, 21L, "reflection",
                "我从这个项目学到的一点", "这里有一个具体设计让我重新想了想。"), 22L, "127.0.0.1");

        verify(contexts).insert(9L, "external_project", 21L, "reflection");
    }
}

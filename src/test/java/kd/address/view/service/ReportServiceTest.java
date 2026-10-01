package kd.address.view.service;

import kd.address.view.common.ConflictException;
import kd.address.view.dto.ReportRequest;
import kd.address.view.entity.CommunityReply;
import kd.address.view.entity.CommunityReport;
import kd.address.view.mapper.CommunityReplyMapper;
import kd.address.view.mapper.CommunityReportMapper;
import kd.address.view.mapper.CommunityTopicMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ReportServiceTest {
    private CommunityReportMapper reports;
    private CommunityReplyMapper replies;
    private ReplyService replyService;
    private ReportService service;

    @BeforeEach
    void setUp() {
        reports = mock(CommunityReportMapper.class);
        replies = mock(CommunityReplyMapper.class);
        replyService = mock(ReplyService.class);
        service = new ReportService(reports, mock(CommunityTopicMapper.class), replies,
                replyService, mock(IdentityRateLimiter.class));
    }

    @Test
    void validReplyReportIsStored() {
        CommunityReply reply = new CommunityReply();
        reply.setStatus("visible");
        when(replies.findById(12L)).thenReturn(reply);

        CommunityReport result = service.submit(3L,
                new ReportRequest("reply", 12L, "包含不合适的内容"), "127.0.0.1");

        assertEquals(3L, result.getReporterAccountId());
        verify(reports).insert(result);
    }

    @Test
    void hidingReportedReplyCallsModerationOnce() {
        CommunityReport report = new CommunityReport();
        report.setId(6L);
        report.setStatus("open");
        report.setTargetType("reply");
        report.setTargetId(12L);
        when(reports.findById(6L)).thenReturn(report);

        service.resolve(6L, true);

        verify(replyService).hide(12L);
        verify(reports).resolve(6L, "hidden");
    }

    @Test
    void processedReportCannotBeResolvedAgain() {
        CommunityReport report = new CommunityReport();
        report.setStatus("dismissed");
        when(reports.findById(6L)).thenReturn(report);

        assertThrows(ConflictException.class, () -> service.resolve(6L, true));
        verify(replyService, never()).hide(anyLong());
        verify(reports, never()).resolve(anyLong(), anyString());
    }
}

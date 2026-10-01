package kd.address.view.service;

import kd.address.view.common.ConflictException;
import kd.address.view.common.NotFoundException;
import kd.address.view.common.PageResponse;
import kd.address.view.dto.ReportRequest;
import kd.address.view.entity.CommunityReply;
import kd.address.view.entity.CommunityReport;
import kd.address.view.entity.CommunityTopic;
import kd.address.view.mapper.CommunityReplyMapper;
import kd.address.view.mapper.CommunityReportMapper;
import kd.address.view.mapper.CommunityTopicMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {
    private final CommunityReportMapper reports;
    private final CommunityTopicMapper topics;
    private final CommunityReplyMapper replies;
    private final ReplyService replyService;
    private final IdentityRateLimiter limiter;

    @Transactional
    public CommunityReport submit(Long accountId, ReportRequest request, String ip) {
        if (request == null || request.targetId() == null || request.targetId() < 1)
            throw new IllegalArgumentException("请选择举报内容");
        if (!"topic".equals(request.targetType()) && !"reply".equals(request.targetType()))
            throw new IllegalArgumentException("举报类型无效");
        String reason = request.reason() == null ? "" : request.reason().trim();
        if (reason.codePointCount(0, reason.length()) < 5 || reason.codePointCount(0, reason.length()) > 500)
            throw new IllegalArgumentException("举报原因需要 5–500 字");
        if ("topic".equals(request.targetType())) {
            CommunityTopic target = topics.findById(request.targetId());
            if (target == null || !"visible".equals(target.getStatus())) throw new NotFoundException("主题不存在");
        } else {
            CommunityReply target = replies.findById(request.targetId());
            if (target == null || !"visible".equals(target.getStatus())) throw new NotFoundException("回复不存在");
        }
        limiter.checkAction("report", accountId, ip, 10, 50, true);
        CommunityReport report = new CommunityReport();
        report.setReporterAccountId(accountId);
        report.setTargetType(request.targetType());
        report.setTargetId(request.targetId());
        report.setReason(reason);
        reports.insert(report);
        return report;
    }

    public PageResponse<CommunityReport> open(int page, int size) {
        if (page < 1 || size < 1 || size > 50) throw new IllegalArgumentException("分页参数无效");
        List<CommunityReport> records = reports.findOpen((page - 1) * size, size);
        return PageResponse.of(records, reports.countOpen(), page, size);
    }

    @Transactional
    public void resolve(Long reportId, boolean hideTarget) {
        CommunityReport report = reports.findById(reportId);
        if (report == null) throw new NotFoundException("举报不存在");
        if (!"open".equals(report.getStatus())) throw new ConflictException("举报已处理");
        if (hideTarget) {
            if ("topic".equals(report.getTargetType())) {
                if (topics.hide(report.getTargetId()) == 0) throw new ConflictException("主题已不可见");
            } else {
                replyService.hide(report.getTargetId());
            }
        }
        reports.resolve(reportId, hideTarget ? "hidden" : "dismissed");
    }
}

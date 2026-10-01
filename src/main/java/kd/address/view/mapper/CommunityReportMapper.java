package kd.address.view.mapper;

import kd.address.view.entity.CommunityReport;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface CommunityReportMapper {
    @Insert("INSERT INTO community_report(reporter_account_id,target_type,target_id,reason) VALUES(#{reporterAccountId},#{targetType},#{targetId},#{reason})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(CommunityReport report);

    @Select("SELECT * FROM community_report WHERE id=#{id} LIMIT 1")
    CommunityReport findById(Long id);

    @Select("SELECT r.*, CASE WHEN r.target_type='topic' THEN r.target_id ELSE reply.topic_id END AS topic_id " +
            "FROM community_report r LEFT JOIN community_reply reply ON r.target_type='reply' AND reply.id=r.target_id " +
            "WHERE r.status='open' ORDER BY r.create_time ASC,r.id ASC LIMIT #{size} OFFSET #{offset}")
    List<CommunityReport> findOpen(@Param("offset") int offset, @Param("size") int size);

    @Select("SELECT COUNT(*) FROM community_report WHERE status='open'")
    long countOpen();

    @Update("UPDATE community_report SET status=#{status} WHERE id=#{id} AND status='open'")
    int resolve(@Param("id") Long id, @Param("status") String status);
}

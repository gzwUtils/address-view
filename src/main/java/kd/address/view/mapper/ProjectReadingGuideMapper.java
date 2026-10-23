package kd.address.view.mapper;

import kd.address.view.entity.ProjectReadingGuide;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ProjectReadingGuideMapper {
    @Select("SELECT * FROM project_reading_guide WHERE external_project_id=#{projectId} LIMIT 1")
    ProjectReadingGuide findByProjectId(Long projectId);

    @Select("SELECT external_project_id FROM project_reading_guide WHERE status='FAILED' AND last_error=#{error}")
    List<Long> findFailedProjectIdsByError(String error);

    @Select("SELECT * FROM project_reading_guide WHERE status='PENDING' ORDER BY queued_at,id LIMIT 1")
    ProjectReadingGuide findNextPending();

    @Update("UPDATE project_reading_guide SET status='FAILED'," +
            "last_error='任务超时或服务中断，请管理员手动重试',completed_at=UTC_TIMESTAMP() " +
            "WHERE status='RUNNING' AND started_at < UTC_TIMESTAMP() - INTERVAL 10 MINUTE")
    int failStaleRuns();

    @Insert("INSERT INTO project_reading_guide(external_project_id,engine_profile_id,status,queued_at,force_regenerate) " +
            "VALUES(#{externalProjectId},#{engineProfileId},'PENDING',UTC_TIMESTAMP(),#{forceRegenerate}) " +
            "ON DUPLICATE KEY UPDATE engine_profile_id=IF(status='RUNNING',engine_profile_id,VALUES(engine_profile_id))," +
            "force_regenerate=IF(status='RUNNING',force_regenerate,GREATEST(force_regenerate,VALUES(force_regenerate)))," +
            "last_error=IF(status='RUNNING',last_error,NULL),queued_at=IF(status='RUNNING',queued_at,UTC_TIMESTAMP())," +
            "status=IF(status='RUNNING',status,'PENDING')")
    void enqueue(@Param("externalProjectId") Long externalProjectId, @Param("engineProfileId") Long engineProfileId,
                 @Param("forceRegenerate") boolean forceRegenerate);

    @Update("UPDATE project_reading_guide SET status='RUNNING',started_at=UTC_TIMESTAMP(),last_error=NULL " +
            "WHERE id=#{id} AND status='PENDING'")
    int claim(Long id);

    @Update("UPDATE project_reading_guide SET status='SUCCESS',source_version=#{sourceVersion},guide_json=#{guideJson}," +
            "generated_engine_profile_id=#{engineProfileId},force_regenerate=0," +
            "last_error=NULL,completed_at=UTC_TIMESTAMP() WHERE id=#{id}")
    int complete(@Param("id") Long id, @Param("sourceVersion") String sourceVersion,
                 @Param("engineProfileId") Long engineProfileId,
                 @Param("guideJson") String guideJson);

    @Update("UPDATE project_reading_guide SET status='FAILED',last_error=#{error},completed_at=UTC_TIMESTAMP() WHERE id=#{id}")
    int fail(@Param("id") Long id, @Param("error") String error);
}

package kd.address.view.mapper;

import kd.address.view.entity.ExternalProject;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ExternalProjectMapper {
    @Select("SELECT p.*,s.display_name AS source_name,s.source_type AS source_type FROM external_project p " +
            "JOIN external_source s ON s.code=p.source_platform " +
            "WHERE p.featured=1 AND s.enabled=1 AND s.deleted=0 ORDER BY s.id,p.display_rank LIMIT 60")
    List<ExternalProject> findFeatured();

    @Select("SELECT p.*,s.display_name AS source_name,s.source_type AS source_type," +
            "g.status AS reading_guide_status,g.update_time AS reading_guide_updated_at " +
            "FROM external_project p LEFT JOIN external_source s ON s.code=p.source_platform " +
            "LEFT JOIN project_reading_guide g ON g.external_project_id=p.id " +
            "ORDER BY p.featured DESC,p.synced_at DESC,p.id DESC LIMIT 120")
    List<ExternalProject> findReadingCandidates();

    @Select("SELECT p.*,s.display_name AS source_name,s.source_type AS source_type FROM external_project p " +
            "LEFT JOIN external_source s ON s.code=p.source_platform WHERE p.id=#{id}")
    ExternalProject findById(@Param("id") Long id);

    @Update("UPDATE external_project SET featured = 0 WHERE featured = 1 AND source_platform=#{sourceCode}")
    void clearFeatured(@Param("sourceCode") String sourceCode);

    @Insert("INSERT INTO external_project (source_platform, source_repo_id, full_name, source_url, description, " +
            "language, license_spdx, star_count, fork_count, repo_created_at, synced_at, display_rank, featured) " +
            "VALUES (#{sourcePlatform}, #{sourceRepoId}, #{fullName}, #{sourceUrl}, #{description}, " +
            "#{language}, #{licenseSpdx}, #{starCount}, #{forkCount}, #{repoCreatedAt}, UTC_TIMESTAMP(), #{displayRank}, 1) " +
            "ON DUPLICATE KEY UPDATE full_name=VALUES(full_name), source_url=VALUES(source_url), " +
            "description=VALUES(description), language=VALUES(language), license_spdx=VALUES(license_spdx), " +
            "star_count=VALUES(star_count), fork_count=VALUES(fork_count), repo_created_at=VALUES(repo_created_at), " +
            "synced_at=UTC_TIMESTAMP(), display_rank=VALUES(display_rank), featured=1")
    void upsert(ExternalProject project);
}

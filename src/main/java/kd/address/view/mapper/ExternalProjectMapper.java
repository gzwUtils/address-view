package kd.address.view.mapper;

import kd.address.view.entity.ExternalProject;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ExternalProjectMapper {
    @Select("SELECT * FROM external_project WHERE featured = 1 ORDER BY display_rank ASC LIMIT 5")
    List<ExternalProject> findFeatured();

    @Select("SELECT COUNT(*) FROM external_project WHERE featured = 1")
    int countFeatured();

    @Update("UPDATE external_project SET featured = 0 WHERE featured = 1")
    void clearFeatured();

    @Insert("INSERT INTO external_project (source_platform, source_repo_id, full_name, source_url, description, " +
            "language, license_spdx, star_count, fork_count, repo_created_at, synced_at, display_rank, featured) " +
            "VALUES (#{sourcePlatform}, #{sourceRepoId}, #{fullName}, #{sourceUrl}, #{description}, " +
            "#{language}, #{licenseSpdx}, #{starCount}, #{forkCount}, #{repoCreatedAt}, NOW(), #{displayRank}, 1) " +
            "ON DUPLICATE KEY UPDATE full_name=VALUES(full_name), source_url=VALUES(source_url), " +
            "description=VALUES(description), language=VALUES(language), license_spdx=VALUES(license_spdx), " +
            "star_count=VALUES(star_count), fork_count=VALUES(fork_count), repo_created_at=VALUES(repo_created_at), " +
            "synced_at=NOW(), display_rank=VALUES(display_rank), featured=1")
    void upsert(ExternalProject project);
}

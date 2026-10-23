package kd.address.view.mapper;

import kd.address.view.entity.ProjectReadingEngineProfile;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ProjectReadingEngineProfileMapper {
    @Select("SELECT * FROM project_reading_engine_profile ORDER BY id")
    List<ProjectReadingEngineProfile> findAll();

    @Select("SELECT * FROM project_reading_engine_profile WHERE id=#{id} LIMIT 1")
    ProjectReadingEngineProfile findById(Long id);

    @Select("SELECT * FROM project_reading_engine_profile WHERE enabled=1 AND default_for_public=1 ORDER BY id DESC LIMIT 1")
    ProjectReadingEngineProfile findDefaultForPublic();

    @Update("UPDATE project_reading_engine_profile SET default_for_public=0")
    void clearPublicDefault();

    @Insert("INSERT INTO project_reading_engine_profile(name,protocol,base_url,model_name,api_key_ciphertext," +
            "timeout_seconds,enabled,default_for_public) VALUES(#{name},#{protocol},#{baseUrl},#{modelName}," +
            "#{apiKeyCiphertext},#{timeoutSeconds},#{enabled},#{defaultForPublic})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(ProjectReadingEngineProfile profile);

    @Update("UPDATE project_reading_engine_profile SET name=#{name},protocol=#{protocol},base_url=#{baseUrl}," +
            "model_name=#{modelName},api_key_ciphertext=#{apiKeyCiphertext},timeout_seconds=#{timeoutSeconds}," +
            "enabled=#{enabled},default_for_public=#{defaultForPublic} WHERE id=#{id}")
    int update(ProjectReadingEngineProfile profile);

    @Delete("DELETE FROM project_reading_engine_profile WHERE id=#{id}")
    int deleteById(Long id);
}

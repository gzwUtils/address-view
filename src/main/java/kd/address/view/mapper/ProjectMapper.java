package kd.address.view.mapper;

import kd.address.view.entity.Project;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ProjectMapper {

    @Select("SELECT * FROM project ORDER BY id DESC")
    List<Project> findAll();

    @Select("SELECT * FROM project WHERE category = #{category} ORDER BY id DESC")
    List<Project> findByCategory(@Param("category") String category);

    @Select("SELECT DISTINCT category FROM project ORDER BY category")
    List<String> findAllCategories();

    @Select("SELECT * FROM project WHERE id = #{id}")
    Project findById(@Param("id") Long id);

    @Insert({
            "<script>",
            "INSERT INTO project (project_name, short_name, platform_url, background_image, category, type, description, owner_id, owner_name, create_time)",
            "VALUES (#{projectName}, #{shortName}, #{platformUrl}, #{backgroundImage}, #{category}, #{type}, #{description}, #{ownerId}, #{ownerName}, now())",
            "</script>"
    })
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    void insert(Project project);

    @Update({
            "<script>",
            "UPDATE project SET ",
            "project_name = #{projectName}, ",
            "short_name = #{shortName}, ",
            "platform_url = #{platformUrl}, ",
            "background_image = #{backgroundImage}, ",
            "category = #{category}, ",
            "type = #{type}, ",
            "description = #{description}, ",
            "owner_id = #{ownerId}, ",
            "owner_name = #{ownerName}, ",
            "update_time = NOW() ",
            "WHERE id = #{id}",
            "</script>"
    })
    void update(Project project);

    @Delete("DELETE FROM project WHERE id = #{id}")
    void deleteById(@Param("id") Long id);
}
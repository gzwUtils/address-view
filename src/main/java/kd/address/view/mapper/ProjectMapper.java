package kd.address.view.mapper;



import kd.address.view.entity.Project;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ProjectMapper {

    @Select("SELECT * FROM project WHERE category = #{category} ORDER BY id DESC")
    List<Project> findByCategory(String category);

    @Select("SELECT DISTINCT category FROM project ORDER BY category")
    List<String> findAllCategories();

    @Select("SELECT * FROM project WHERE id = #{id}")
    Project findById(Long id);

    @Insert({
            "<script>",
            "INSERT INTO project (project_name, short_name, platform_url, background_image, category, type, description,create_time)",
            "VALUES (#{projectName}, #{shortName}, #{platformUrl}, #{backgroundImage}, #{category}, #{type}, #{description},now())",
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
            "update_time = NOW() ",
            "WHERE id = #{id}",
            "</script>"
    })
    void update(Project project);

    @Delete("DELETE FROM project WHERE id = #{id}")
    void deleteById(Long id);
}

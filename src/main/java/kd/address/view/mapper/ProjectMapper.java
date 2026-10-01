package kd.address.view.mapper;

import kd.address.view.entity.Project;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ProjectMapper {

    String PROJECT_COLUMNS = "p.id,p.project_name,p.short_name,p.platform_url,p.background_image,p.category,p.type,p.description,p.owner_id,COALESCE(a.nickname,p.owner_name) AS owner_name,p.owner_account_id,p.create_time,p.update_time ";
    String PROJECT_FROM = "FROM project p LEFT JOIN community_account a ON a.id=p.owner_account_id ";

    @Select("SELECT " + PROJECT_COLUMNS + PROJECT_FROM + "ORDER BY p.id DESC")
    List<Project> findAll();

    @Select("SELECT " + PROJECT_COLUMNS + PROJECT_FROM + "WHERE p.category = #{category} ORDER BY p.id DESC")
    List<Project> findByCategory(@Param("category") String category);

    @Select("SELECT DISTINCT category FROM project ORDER BY category")
    List<String> findAllCategories();

    @Select("SELECT " + PROJECT_COLUMNS + PROJECT_FROM + "WHERE p.id = #{id}")
    Project findById(@Param("id") Long id);

    @Insert({
            "<script>",
            "INSERT INTO project (project_name, short_name, platform_url, background_image, category, type, description, owner_name, owner_account_id, create_time)",
            "VALUES (#{projectName}, #{shortName}, #{platformUrl}, #{backgroundImage}, #{category}, #{type}, #{description}, #{ownerName}, #{ownerAccountId}, now())",
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
    void updateContent(Project project);

    @Delete("DELETE FROM project WHERE id = #{id}")
    void deleteById(@Param("id") Long id);

    @Update("UPDATE project SET owner_account_id=#{accountId} WHERE id=#{id} AND owner_account_id IS NULL")
    int assignLegacyOwner(@Param("id") Long id, @Param("accountId") Long accountId);
}

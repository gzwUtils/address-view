package kd.address.view.mapper;

import kd.address.view.entity.GrowthCapsule;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface GrowthCapsuleMapper {

    @Select("SELECT * FROM user_growth_capsule WHERE user_id = #{userId} LIMIT 1")
    GrowthCapsule findByUserId(@Param("userId") String userId);

    @Insert("INSERT INTO user_growth_capsule (user_id, capsule_name, tagline, ai_brief, create_time, update_time) " +
            "VALUES (#{userId}, #{capsuleName}, #{tagline}, #{aiBrief}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    void insert(GrowthCapsule capsule);

    @Update("UPDATE user_growth_capsule SET capsule_name = #{capsuleName}, tagline = #{tagline}, ai_brief = #{aiBrief}, update_time = NOW() WHERE id = #{id}")
    void update(GrowthCapsule capsule);
}

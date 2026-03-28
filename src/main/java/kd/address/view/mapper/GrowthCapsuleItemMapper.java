package kd.address.view.mapper;

import kd.address.view.entity.GrowthCapsuleItem;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface GrowthCapsuleItemMapper {

    @Select("SELECT * FROM user_growth_item WHERE capsule_id = #{capsuleId} ORDER BY update_time DESC, id DESC")
    List<GrowthCapsuleItem> findByCapsuleId(@Param("capsuleId") Long capsuleId);

    @Select("SELECT * FROM user_growth_item WHERE capsule_id = #{capsuleId} AND source_kind = #{sourceKind} AND source_id = #{sourceId} LIMIT 1")
    GrowthCapsuleItem findByCapsuleAndSource(@Param("capsuleId") Long capsuleId,
                                             @Param("sourceKind") String sourceKind,
                                             @Param("sourceId") Long sourceId);

    @Select("SELECT * FROM user_growth_item WHERE id = #{id} LIMIT 1")
    GrowthCapsuleItem findById(@Param("id") Long id);

    @Insert("INSERT INTO user_growth_item (" +
            "capsule_id, source_kind, source_id, title, subtitle, note, action_plan, value_summary, first_step, seven_day_plan, status, expected_minutes, create_time, update_time" +
            ") VALUES (" +
            "#{capsuleId}, #{sourceKind}, #{sourceId}, #{title}, #{subtitle}, #{note}, #{actionPlan}, #{valueSummary}, #{firstStep}, #{sevenDayPlan}, #{status}, #{expectedMinutes}, NOW(), NOW()" +
            ")")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    void insert(GrowthCapsuleItem item);

    @Update("UPDATE user_growth_item SET note = #{note}, status = #{status}, update_time = NOW() WHERE id = #{id}")
    void updateProgress(GrowthCapsuleItem item);

    @Update("UPDATE user_growth_item SET title = #{title}, subtitle = #{subtitle}, note = #{note}, action_plan = #{actionPlan}, value_summary = #{valueSummary}, first_step = #{firstStep}, seven_day_plan = #{sevenDayPlan}, status = #{status}, expected_minutes = #{expectedMinutes}, update_time = NOW() WHERE id = #{id}")
    void updateContent(GrowthCapsuleItem item);
}

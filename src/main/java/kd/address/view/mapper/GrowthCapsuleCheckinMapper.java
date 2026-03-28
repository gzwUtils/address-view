package kd.address.view.mapper;

import kd.address.view.entity.GrowthCapsuleCheckin;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface GrowthCapsuleCheckinMapper {

    @Insert("INSERT INTO user_growth_checkin (item_id, content, mood, create_time) VALUES (#{itemId}, #{content}, #{mood}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    void insert(GrowthCapsuleCheckin checkin);

    @Select("SELECT * FROM user_growth_checkin WHERE item_id = #{itemId} ORDER BY id DESC")
    List<GrowthCapsuleCheckin> findByItemId(@Param("itemId") Long itemId);

    @Select("SELECT COUNT(1) FROM user_growth_checkin WHERE item_id = #{itemId}")
    int countByItemId(@Param("itemId") Long itemId);

    @Select("SELECT DATE(gc.create_time) FROM user_growth_checkin gc " +
            "INNER JOIN user_growth_item gi ON gc.item_id = gi.id " +
            "WHERE gi.capsule_id = #{capsuleId} " +
            "GROUP BY DATE(gc.create_time) " +
            "ORDER BY DATE(gc.create_time) DESC")
    List<LocalDate> findCheckinDatesByCapsuleId(@Param("capsuleId") Long capsuleId);

    @Select("SELECT COUNT(1) FROM user_growth_checkin gc " +
            "INNER JOIN user_growth_item gi ON gc.item_id = gi.id " +
            "WHERE gi.capsule_id = #{capsuleId} AND DATE(gc.create_time) = CURRENT_DATE")
    int countTodayByCapsuleId(@Param("capsuleId") Long capsuleId);
}

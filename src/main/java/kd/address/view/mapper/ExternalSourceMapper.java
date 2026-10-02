package kd.address.view.mapper;

import kd.address.view.entity.ExternalSource;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ExternalSourceMapper {
    @Select("SELECT * FROM external_source WHERE deleted=0 ORDER BY id")
    List<ExternalSource> findAll();

    @Select("SELECT * FROM external_source WHERE id=#{id} AND deleted=0")
    ExternalSource findById(@Param("id") Long id);

    @Select("SELECT * FROM external_source WHERE enabled=1 AND deleted=0 " +
            "AND (next_run_at IS NULL OR next_run_at<=#{now}) ORDER BY id LIMIT 20")
    List<ExternalSource> findDue(@Param("now") LocalDateTime now);

    @Insert("INSERT INTO external_source (code,display_name,source_type,feed_url,query_text,period_days," +
            "min_stars,max_items,interval_hours,enabled,next_run_at) VALUES (#{code},#{displayName}," +
            "#{sourceType},#{feedUrl},#{queryText},#{periodDays},#{minStars},#{maxItems}," +
            "#{intervalHours},#{enabled},NULL)")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    void insert(ExternalSource source);

    @Update("UPDATE external_source SET display_name=#{displayName},source_type=#{sourceType}," +
            "feed_url=#{feedUrl},query_text=#{queryText},period_days=#{periodDays},min_stars=#{minStars}," +
            "max_items=#{maxItems},interval_hours=#{intervalHours},enabled=#{enabled}," +
            "next_run_at=CASE WHEN #{enabled}=1 THEN NULL ELSE next_run_at END WHERE id=#{id} AND deleted=0")
    int update(ExternalSource source);

    @Update("UPDATE external_source SET deleted=1,enabled=0 WHERE id=#{id} AND deleted=0")
    int softDelete(@Param("id") Long id);

    @Update("UPDATE external_source SET last_status=#{status},last_error=#{error},last_run_at=#{runAt}," +
            "next_run_at=#{nextRunAt} WHERE id=#{id} AND deleted=0")
    void updateRun(@Param("id") Long id, @Param("status") String status, @Param("error") String error,
                   @Param("runAt") LocalDateTime runAt, @Param("nextRunAt") LocalDateTime nextRunAt);
}

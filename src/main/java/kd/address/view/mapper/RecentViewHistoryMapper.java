package kd.address.view.mapper;

import kd.address.view.entity.RecentViewHistory;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface RecentViewHistoryMapper {

    @Select("SELECT * FROM recent_view_history WHERE client_id = #{clientId} AND kind = #{kind} AND target_id = #{targetId} LIMIT 1")
    RecentViewHistory findByClientAndTarget(@Param("clientId") String clientId,
                                            @Param("kind") String kind,
                                            @Param("targetId") Long targetId);

    @Insert("INSERT INTO recent_view_history (client_id, kind, target_id, title, subtitle, view_time, create_time, update_time) " +
            "VALUES (#{clientId}, #{kind}, #{targetId}, #{title}, #{subtitle}, NOW(), NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    void insert(RecentViewHistory history);

    @Update("UPDATE recent_view_history SET title = #{title}, subtitle = #{subtitle}, view_time = NOW(), update_time = NOW() WHERE id = #{id}")
    void touch(RecentViewHistory history);

    @Select("SELECT * FROM recent_view_history WHERE client_id = #{clientId} ORDER BY view_time DESC, id DESC LIMIT #{limit}")
    List<RecentViewHistory> findRecentByClientId(@Param("clientId") String clientId, @Param("limit") int limit);
}

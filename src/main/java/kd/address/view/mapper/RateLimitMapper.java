package kd.address.view.mapper;

import org.apache.ibatis.annotations.*;
import java.time.LocalDateTime;

@Mapper
public interface RateLimitMapper {
    @Insert("INSERT INTO community_rate_limit(action_key,window_start,hit_count) VALUES(#{key},#{window},1) ON DUPLICATE KEY UPDATE hit_count=hit_count+1")
    void hit(@Param("key") String key, @Param("window") LocalDateTime window);

    @Select("SELECT hit_count FROM community_rate_limit WHERE action_key=#{key} AND window_start=#{window}")
    Integer count(@Param("key") String key, @Param("window") LocalDateTime window);
}

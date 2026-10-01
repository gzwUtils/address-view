package kd.address.view.mapper;

import kd.address.view.entity.GuestSession;
import org.apache.ibatis.annotations.*;
import java.time.LocalDateTime;

@Mapper
public interface GuestSessionMapper {
    @Insert("INSERT INTO community_session(account_id,token_hash,expires_at) VALUES(#{accountId},#{tokenHash},#{expiresAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(GuestSession session);

    @Select("SELECT * FROM community_session WHERE token_hash=#{hash} AND revoked_at IS NULL AND expires_at>UTC_TIMESTAMP() LIMIT 1")
    GuestSession findByTokenHash(String hash);

    @Update("UPDATE community_session SET expires_at=#{expiresAt} WHERE id=#{id} AND revoked_at IS NULL")
    int renew(@Param("id") Long id, @Param("expiresAt") LocalDateTime expiresAt);
}

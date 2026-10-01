package kd.address.view.mapper;

import kd.address.view.entity.AdminSession;
import org.apache.ibatis.annotations.*;

@Mapper
public interface AdminSessionMapper {
    @Insert("INSERT INTO community_admin_session(token_hash,expires_at) VALUES(#{tokenHash},#{expiresAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(AdminSession session);

    @Select("SELECT * FROM community_admin_session WHERE token_hash=#{hash} AND revoked_at IS NULL AND expires_at>UTC_TIMESTAMP() LIMIT 1")
    AdminSession findActive(String hash);
}

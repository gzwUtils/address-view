package kd.address.view.mapper;

import kd.address.view.entity.GuestAccount;
import org.apache.ibatis.annotations.*;

@Mapper
public interface GuestAccountMapper {
    @Insert("INSERT INTO community_account(public_id,nickname,recovery_hash,status) VALUES(#{publicId},#{nickname},#{recoveryHash},'active')")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(GuestAccount account);

    @Select("SELECT * FROM community_account WHERE public_id=#{publicId} LIMIT 1")
    GuestAccount findByPublicId(String publicId);

    @Select("SELECT * FROM community_account WHERE nickname=#{nickname} LIMIT 1")
    GuestAccount findByNickname(String nickname);

    @Select("SELECT * FROM community_account WHERE id=#{id} LIMIT 1")
    GuestAccount findById(Long id);

    @Update("UPDATE community_account SET nickname=#{nickname} WHERE id=#{id}")
    int updateNickname(@Param("id") Long id, @Param("nickname") String nickname);

    @Update("UPDATE community_account SET recovery_hash=#{hash} WHERE id=#{id}")
    int updateRecoveryHash(@Param("id") Long id, @Param("hash") String hash);
}

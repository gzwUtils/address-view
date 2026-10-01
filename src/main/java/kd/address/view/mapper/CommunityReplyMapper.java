package kd.address.view.mapper;

import kd.address.view.entity.CommunityReply;
import org.apache.ibatis.annotations.*;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CommunityReplyMapper {
    String COLUMNS = "r.*,a.nickname AS author_nickname ";
    String TABLES = "FROM community_reply r JOIN community_account a ON a.id=r.author_account_id ";

    @Select("SELECT " + COLUMNS + TABLES + "WHERE r.topic_id=#{topicId} ORDER BY r.floor_no ASC LIMIT #{size} OFFSET #{offset}")
    List<CommunityReply> findPage(@Param("topicId") Long topicId, @Param("offset") int offset, @Param("size") int size);

    @Select("SELECT COUNT(*) FROM community_reply WHERE topic_id=#{topicId}")
    long countAll(Long topicId);

    @Select("SELECT " + COLUMNS + TABLES + "WHERE r.id=#{id} LIMIT 1")
    CommunityReply findById(Long id);

    @Select("SELECT " + COLUMNS + TABLES + "WHERE r.topic_id=#{topicId} AND r.floor_no=#{floorNo} LIMIT 1")
    CommunityReply findByTopicAndFloor(@Param("topicId") Long topicId, @Param("floorNo") int floorNo);

    @Insert("INSERT INTO community_reply(topic_id,author_account_id,reply_to_id,floor_no,body) " +
            "VALUES(#{topicId},#{authorAccountId},#{replyToId},#{floorNo},#{body})")
    int insert(@Param("topicId") Long topicId, @Param("authorAccountId") Long authorAccountId,
               @Param("replyToId") Long replyToId, @Param("floorNo") int floorNo, @Param("body") String body);

    @Update("UPDATE community_reply SET body=#{body} WHERE id=#{id} AND status='visible'")
    int updateBody(@Param("id") Long id, @Param("body") String body);

    @Update("UPDATE community_reply SET status='deleted',body='' WHERE id=#{id} AND status='visible'")
    int softDelete(Long id);

    @Update("UPDATE community_reply SET status='hidden',body='' WHERE id=#{id} AND status='visible'")
    int hide(Long id);

    @Select("SELECT COUNT(*) FROM community_reply WHERE topic_id=#{topicId} AND status='visible'")
    int countVisible(Long topicId);

    @Select("SELECT MAX(create_time) FROM community_reply WHERE topic_id=#{topicId} AND status='visible'")
    LocalDateTime latestVisibleTime(Long topicId);

    @Select("SELECT COUNT(*) FROM community_reply WHERE topic_id=#{topicId} AND status='visible' AND id>#{afterId}")
    long countAfter(@Param("topicId") Long topicId, @Param("afterId") Long afterId);
}

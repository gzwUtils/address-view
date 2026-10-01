package kd.address.view.mapper;

import kd.address.view.entity.CommunityTopic;
import org.apache.ibatis.annotations.*;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CommunityTopicMapper {
    String COLUMNS = "t.*,b.code AS board_code,b.name AS board_name,a.nickname AS author_nickname,p.project_name ";
    String TABLES = "FROM community_topic t JOIN community_board b ON b.id=t.board_id " +
            "JOIN community_account a ON a.id=t.author_account_id LEFT JOIN project p ON p.id=t.project_id ";
    String FILTER = "WHERE t.status='visible' AND b.status='visible' " +
            "<if test='boardCode != null and boardCode != \"\"'>AND b.code=#{boardCode} </if>" +
            "<if test='projectId != null'>AND t.project_id=#{projectId} </if>" +
            "<if test='keyword != null and keyword != \"\"'>AND (INSTR(t.title,#{keyword})>0 OR INSTR(t.body,#{keyword})>0) </if>";

    @Select("<script>SELECT " + COLUMNS + TABLES + FILTER +
            "<choose><when test='sort == \"new\"'>ORDER BY t.create_time DESC,t.id DESC </when>" +
            "<otherwise>ORDER BY COALESCE(t.last_reply_time,t.create_time) DESC,t.id DESC </otherwise></choose>" +
            "LIMIT #{size} OFFSET #{offset}</script>")
    List<CommunityTopic> findPage(@Param("boardCode") String boardCode, @Param("projectId") Long projectId,
                                  @Param("keyword") String keyword, @Param("sort") String sort,
                                  @Param("offset") int offset, @Param("size") int size);

    @Select("<script>SELECT COUNT(*) " + TABLES + FILTER + "</script>")
    long count(@Param("boardCode") String boardCode, @Param("projectId") Long projectId,
               @Param("keyword") String keyword);

    @Select("SELECT " + COLUMNS + TABLES + "WHERE t.id=#{id} LIMIT 1")
    CommunityTopic findById(Long id);

    @Select("SELECT * FROM community_topic WHERE id=#{id} FOR UPDATE")
    CommunityTopic lockById(Long id);

    @Insert("INSERT INTO community_topic(board_id,author_account_id,project_id,title,body) " +
            "VALUES(#{boardId},#{authorAccountId},#{projectId},#{title},#{body})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(CommunityTopic topic);

    @Update("UPDATE community_topic SET board_id=#{boardId},project_id=#{projectId},title=#{title},body=#{body} WHERE id=#{id} AND status='visible'")
    int updateContent(CommunityTopic topic);

    @Update("UPDATE community_topic SET status='deleted' WHERE id=#{id} AND status='visible'")
    int softDelete(Long id);

    @Update("UPDATE community_topic SET status='hidden' WHERE id=#{id} AND status='visible'")
    int hide(Long id);

    @Update("UPDATE community_topic SET next_floor=#{nextFloor},reply_count=#{replyCount},last_reply_time=#{lastReplyTime} WHERE id=#{id}")
    int updateReplyStats(@Param("id") Long id, @Param("nextFloor") int nextFloor,
                         @Param("replyCount") int replyCount, @Param("lastReplyTime") LocalDateTime lastReplyTime);

    @Update("UPDATE community_topic SET reply_count=#{replyCount},last_reply_time=#{lastReplyTime} WHERE id=#{id}")
    int setReplyStats(@Param("id") Long id, @Param("replyCount") int replyCount,
                      @Param("lastReplyTime") LocalDateTime lastReplyTime);

    @Select("SELECT " + COLUMNS + TABLES + "WHERE t.status='visible' AND b.status='visible' AND " +
            "(t.author_account_id=#{accountId} OR EXISTS(SELECT 1 FROM community_reply r WHERE r.topic_id=t.id AND r.author_account_id=#{accountId} AND r.status='visible')) " +
            "ORDER BY COALESCE(t.last_reply_time,t.create_time) DESC,t.id DESC LIMIT #{size} OFFSET #{offset}")
    List<CommunityTopic> findMine(@Param("accountId") Long accountId, @Param("offset") int offset, @Param("size") int size);

    @Select("SELECT COUNT(*) FROM community_topic t JOIN community_board b ON b.id=t.board_id WHERE t.status='visible' AND b.status='visible' AND " +
            "(t.author_account_id=#{accountId} OR EXISTS(SELECT 1 FROM community_reply r WHERE r.topic_id=t.id AND r.author_account_id=#{accountId} AND r.status='visible'))")
    long countMine(Long accountId);
}

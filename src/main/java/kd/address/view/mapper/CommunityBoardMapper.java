package kd.address.view.mapper;

import kd.address.view.entity.CommunityBoard;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface CommunityBoardMapper {
    @Select("SELECT b.*, (SELECT COUNT(*) FROM community_topic t WHERE t.board_id=b.id AND t.status='visible') AS topic_count, " +
            "(SELECT MAX(COALESCE(t.last_reply_time,t.create_time)) FROM community_topic t WHERE t.board_id=b.id AND t.status='visible') AS last_activity " +
            "FROM community_board b WHERE b.status='visible' ORDER BY b.sort_order,b.id")
    List<CommunityBoard> findVisible();

    @Select("SELECT * FROM community_board WHERE code=#{code} AND status='visible' LIMIT 1")
    CommunityBoard findVisibleByCode(String code);
}

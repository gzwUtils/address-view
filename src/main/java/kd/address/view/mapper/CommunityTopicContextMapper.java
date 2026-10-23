package kd.address.view.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CommunityTopicContextMapper {
    @Delete("DELETE FROM community_topic_context WHERE topic_id=#{topicId}")
    void deleteByTopicId(@Param("topicId") Long topicId);

    @Insert("INSERT INTO community_topic_context(topic_id,reference_type,reference_id,content_type) " +
            "VALUES(#{topicId},#{referenceType},#{referenceId},#{contentType})")
    void insert(@Param("topicId") Long topicId,
                @Param("referenceType") String referenceType,
                @Param("referenceId") Long referenceId,
                @Param("contentType") String contentType);
}

package kd.address.view.mapper;

import kd.address.view.entity.PortalResource;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface PortalResourceMapper {

    @Select("SELECT * FROM portal_resource WHERE deleted = 0 ORDER BY sort_order ASC, id ASC")
    List<PortalResource> findAllActive();

    @Select("SELECT * FROM portal_resource WHERE id = #{id} AND deleted = 0")
    PortalResource findById(Long id);

    @Insert("INSERT INTO portal_resource (" +
            "resource_code, kind, title, name, excerpt, description, category, type, owner, author, meta, resource_date, status, version, updated_at, entry_url, cover_image, content_body, tags, capabilities, sort_order, deleted, create_time, update_time" +
            ") VALUES (" +
            "#{resourceCode}, #{kind}, #{title}, #{name}, #{excerpt}, #{description}, #{category}, #{type}, #{owner}, #{author}, #{meta}, #{resourceDate}, #{status}, #{version}, #{updatedAt}, #{entryUrl}, #{coverImage}, #{contentBody}, #{tags}, #{capabilities}, #{sortOrder}, #{deleted}, NOW(), NOW()" +
            ")")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    void insert(PortalResource resource);

    @Update("UPDATE portal_resource SET " +
            "resource_code = #{resourceCode}, " +
            "kind = #{kind}, " +
            "title = #{title}, " +
            "name = #{name}, " +
            "excerpt = #{excerpt}, " +
            "description = #{description}, " +
            "category = #{category}, " +
            "type = #{type}, " +
            "owner = #{owner}, " +
            "author = #{author}, " +
            "meta = #{meta}, " +
            "resource_date = #{resourceDate}, " +
            "status = #{status}, " +
            "version = #{version}, " +
            "updated_at = #{updatedAt}, " +
            "entry_url = #{entryUrl}, " +
            "cover_image = #{coverImage}, " +
            "content_body = #{contentBody}, " +
            "tags = #{tags}, " +
            "capabilities = #{capabilities}, " +
            "sort_order = #{sortOrder}, " +
            "update_time = NOW() " +
            "WHERE id = #{id}")
    void update(PortalResource resource);

    @Update("UPDATE portal_resource SET deleted = 1, update_time = NOW() WHERE id = #{id}")
    void softDeleteById(Long id);
}

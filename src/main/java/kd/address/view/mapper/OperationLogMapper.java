package kd.address.view.mapper;

import kd.address.view.entity.OperationLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OperationLogMapper {

    @Insert("INSERT INTO operation_log (module, action, target_type, target_name, operator_name, detail, create_time) " +
            "VALUES (#{module}, #{action}, #{targetType}, #{targetName}, #{operatorName}, #{detail}, NOW())")
    void insert(OperationLog log);

    @Select("SELECT * FROM operation_log ORDER BY id DESC LIMIT #{limit}")
    List<OperationLog> findRecent(int limit);
}

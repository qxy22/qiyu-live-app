package org.qiyu.live.id.generate.provider.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.qiyu.live.id.generate.provider.dao.po.IdBuilderPO;

import java.util.List;


/**
 * @Author idea
 * @Date: Created in 15:46 2023/5/24
 * @Description
 */
@Mapper
public interface IdBuilderMapper extends BaseMapper<IdBuilderPO> {

    @Update("update t_id_generate_config set current_start = next_threshold,next_threshold=current_start+step,version=version+1 where id=#{code}")
    Integer updateNewVersion(@Param("code") Integer code);

    @Update("UPDATE t_id_generate_config set next_threshold=#{nextThreshold},current_start=#{currentStart},version=version+1,update_time=now() where id=#{id} and version=#{version}")
    Integer updateCurrentThreshold(@Param("nextThreshold") long nextThreshold, @Param("currentStart") long currentStart, @Param("id") int id, @Param("version") int version);

    @Select("select * from t_id_generate_config")
    List<IdBuilderPO> selectAll();
}

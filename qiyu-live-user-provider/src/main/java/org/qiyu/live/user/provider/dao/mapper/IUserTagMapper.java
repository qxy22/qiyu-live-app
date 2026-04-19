package org.qiyu.live.user.provider.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.qiyu.live.user.provider.dao.po.UserTagPO;

@Mapper
public interface IUserTagMapper extends BaseMapper<UserTagPO> {

    @Insert("""
            INSERT IGNORE INTO t_user_tag
            (user_id, tag_info_01, tag_info_02, tag_info_03, create_time, update_time)
            VALUES (#{userId}, 0, 0, 0, NOW(), NOW())
            """)
    int initUserTag(@Param("userId") Long userId);

    @Update("update t_user_tag set ${fieldName} = ${fieldName} | #{tag}, update_time = now() where user_id = #{userId} and ${fieldName} & #{tag} = 0")
    int setTag(@Param("userId") Long userId, @Param("fieldName") String fieldName, @Param("tag") long tag);

    @Update("update t_user_tag set ${fieldName} = ${fieldName} & ~#{tag}, update_time = now() where user_id = #{userId} and ${fieldName} & #{tag} = #{tag}")
    int cancelTag(@Param("userId") Long userId, @Param("fieldName") String fieldName, @Param("tag") long tag);
}

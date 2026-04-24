package org.qiyu.live.user.provider.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.qiyu.live.user.provider.dao.po.UserPhonePO;

@Mapper
public interface IUserPhoneMapper extends BaseMapper<UserPhonePO> {

    @Select("select id, user_id, phone, status, create_time, update_time from t_user_phone where phone = #{phone} and status = 1 limit 1")
    UserPhonePO selectByPhone(@Param("phone") String phone);

    @Select("select id, user_id, phone, status, create_time, update_time from t_user_phone where user_id = #{userId} and status = 1 limit 1")
    UserPhonePO selectByUserId(@Param("userId") Long userId);
}

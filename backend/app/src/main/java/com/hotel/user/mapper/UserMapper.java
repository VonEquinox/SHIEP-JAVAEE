package com.hotel.user.mapper;

import com.hotel.user.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface UserMapper {

    @Select("SELECT COUNT(*) FROM t_user WHERE username = #{username}")
    long countByUsername(String username);

    @Insert("INSERT INTO t_user (username, password, phone, id_card) "
            + "VALUES (#{username}, #{password}, #{phone}, #{idCard})")
    @Options(useGeneratedKeys = true, keyProperty = "userId")
    int insert(User user);

    @Select("SELECT * FROM t_user WHERE username = #{username}")
    User selectByUsername(String username);

    @Select("SELECT * FROM t_user WHERE user_id = #{userId}")
    User selectById(Integer userId);

    @Update("<script>"
            + "UPDATE t_user"
            + "<set>"
            + "<if test='phone != null'>phone = #{phone},</if>"
            + "<if test='password != null'>password = #{password},</if>"
            + "<if test='idCard != null'>id_card = #{idCard},</if>"
            + "</set>"
            + "WHERE user_id = #{userId}"
            + "</script>")
    int updateProfile(User user);

    @Select("SELECT * FROM t_user ORDER BY created_at DESC")
    List<User> selectAll();
}

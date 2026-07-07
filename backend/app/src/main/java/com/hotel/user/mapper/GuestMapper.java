package com.hotel.user.mapper;

import com.hotel.user.entity.Guest;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface GuestMapper {

    @Select("SELECT * FROM t_guest WHERE user_id = #{userId} ORDER BY guest_id DESC")
    List<Guest> selectByUserId(Integer userId);

    @Select("SELECT * FROM t_guest WHERE guest_id = #{guestId}")
    Guest selectById(Integer guestId);

    @Insert("INSERT INTO t_guest (user_id, name, id_card, gender, birth_date, occupation, education, income_level) "
            + "VALUES (#{userId}, #{name}, #{idCard}, #{gender}, #{birthDate}, #{occupation}, #{education}, #{incomeLevel})")
    @Options(useGeneratedKeys = true, keyProperty = "guestId")
    int insert(Guest guest);

    @Update("UPDATE t_guest SET name = #{name}, id_card = #{idCard}, gender = #{gender}, "
            + "birth_date = #{birthDate}, occupation = #{occupation}, education = #{education}, income_level = #{incomeLevel} "
            + "WHERE guest_id = #{guestId} AND user_id = #{userId}")
    int update(Guest guest);

    @Delete("DELETE FROM t_guest WHERE guest_id = #{guestId}")
    int deleteById(Integer guestId);
}

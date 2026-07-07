package com.hotel.hotel.mapper;

import com.hotel.hotel.entity.RoomType;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface RoomTypeMapper {

    @Select("SELECT * FROM t_room_type WHERE room_type_id = #{roomTypeId}")
    RoomType selectById(Integer roomTypeId);

    @Insert("INSERT INTO t_room_type (hotel_id, type_name, price, capacity, total_count, available_count) "
            + "VALUES (#{hotelId}, #{typeName}, #{price}, #{capacity}, #{totalCount}, #{availableCount})")
    @Options(useGeneratedKeys = true, keyProperty = "roomTypeId")
    int insert(RoomType room);

    @Update("UPDATE t_room_type SET hotel_id = #{hotelId}, type_name = #{typeName}, price = #{price}, "
            + "capacity = #{capacity}, total_count = #{totalCount}, available_count = #{availableCount} "
            + "WHERE room_type_id = #{roomTypeId}")
    int update(RoomType room);

    @Delete("DELETE FROM t_room_type WHERE room_type_id = #{roomTypeId}")
    int deleteById(Integer roomTypeId);

    int deleteBatchIds(@Param("ids") List<Integer> ids);
}

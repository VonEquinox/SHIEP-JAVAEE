package com.hotel.hotel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hotel.hotel.entity.Hotel;
import com.hotel.hotel.entity.HotelRoom;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface HotelMapper extends BaseMapper<Hotel> {

    /** 组合条件搜索酒店，复杂 SQL 在 HotelMapper.xml */
    List<Hotel> search(@Param("city") String city,
                       @Param("district") String district,
                       @Param("name") String name,
                       @Param("priceMin") BigDecimal priceMin,
                       @Param("priceMax") BigDecimal priceMax);

    /** 酒店客房详情：按需求文档要求走视图 v_hotel_room */
    @Select("SELECT * FROM v_hotel_room WHERE hotel_id = #{hotelId}")
    List<HotelRoom> selectRooms(Integer hotelId);
}

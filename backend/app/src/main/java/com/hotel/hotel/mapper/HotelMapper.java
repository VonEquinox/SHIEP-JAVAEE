package com.hotel.hotel.mapper;

import com.hotel.hotel.entity.Hotel;
import com.hotel.hotel.entity.HotelRoom;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface HotelMapper {

    List<Hotel> search(@Param("city") String city,
                       @Param("district") String district,
                       @Param("name") String name,
                       @Param("priceMin") BigDecimal priceMin,
                       @Param("priceMax") BigDecimal priceMax);

    @Select("SELECT * FROM v_hotel_room WHERE hotel_id = #{hotelId}")
    List<HotelRoom> selectRooms(Integer hotelId);

    @Select("SELECT * FROM t_hotel ORDER BY city, district, hotel_name")
    List<Hotel> selectAll();

    @Insert("INSERT INTO t_hotel (hotel_name, city, district, address, star, phone) "
            + "VALUES (#{hotelName}, #{city}, #{district}, #{address}, #{star}, #{phone})")
    @Options(useGeneratedKeys = true, keyProperty = "hotelId")
    int insert(Hotel hotel);

    @Update("UPDATE t_hotel SET hotel_name = #{hotelName}, city = #{city}, district = #{district}, "
            + "address = #{address}, star = #{star}, phone = #{phone} WHERE hotel_id = #{hotelId}")
    int update(Hotel hotel);

    @Delete("DELETE FROM t_hotel WHERE hotel_id = #{hotelId}")
    int deleteById(Integer hotelId);
}

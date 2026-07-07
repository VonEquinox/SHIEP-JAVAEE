package com.hotel.order.mapper;

import com.hotel.order.entity.Review;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface ReviewMapper {

    @Select("SELECT COUNT(*) FROM t_review WHERE order_id = #{orderId}")
    long countByOrderId(Integer orderId);

    @Insert("INSERT INTO t_review (order_id, user_id, score, content) "
            + "VALUES (#{orderId}, #{userId}, #{score}, #{content})")
    @Options(useGeneratedKeys = true, keyProperty = "reviewId")
    int insert(Review review);

    @Select("SELECT u.username, r.score, r.content, r.created_at AS createdAt "
            + "FROM t_review r "
            + "JOIN t_order o ON o.order_id = r.order_id "
            + "JOIN t_user u ON u.user_id = r.user_id "
            + "WHERE o.hotel_id = #{hotelId} ORDER BY r.created_at DESC")
    List<Map<String, Object>> listByHotel(Integer hotelId);
}

package com.hotel.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hotel.order.entity.Review;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface ReviewMapper extends BaseMapper<Review> {

    /** 某酒店的评价列表（酒店详情页展示） */
    @Select("SELECT u.username, r.score, r.content, r.created_at AS createdAt "
            + "FROM t_review r "
            + "JOIN t_order o ON o.order_id = r.order_id "
            + "JOIN t_user u ON u.user_id = r.user_id "
            + "WHERE o.hotel_id = #{hotelId} ORDER BY r.created_at DESC")
    List<Map<String, Object>> listByHotel(Integer hotelId);
}

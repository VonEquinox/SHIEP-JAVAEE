package com.hotel.order.mapper;

import com.hotel.order.entity.Order;
import com.hotel.order.entity.OrderDetailRow;
import com.hotel.order.entity.OrderListItem;
import com.hotel.order.entity.RoomInfo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface OrderMapper {

    @Select("SELECT room_type_id, hotel_id, type_name, price, capacity, available_count "
            + "FROM t_room_type WHERE room_type_id = #{roomTypeId}")
    RoomInfo selectRoomInfo(Integer roomTypeId);

    @Select("SELECT level FROM t_user WHERE user_id = #{userId}")
    String selectUserLevel(Integer userId);

    @Insert("INSERT INTO t_order (order_no, user_id, hotel_id, check_in_date, check_out_date, total_price, pay_price, status) "
            + "VALUES (#{orderNo}, #{userId}, #{hotelId}, #{checkInDate}, #{checkOutDate}, "
            + "#{totalPrice}, #{payPrice}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "orderId")
    int insert(Order order);

    @Select("SELECT * FROM t_order WHERE order_id = #{orderId}")
    Order selectById(Integer orderId);

    @Update("UPDATE t_order SET status = #{status} WHERE order_id = #{orderId}")
    int updateStatus(Order order);

    List<OrderListItem> callUserOrders(@Param("userId") Integer userId);

    List<OrderListItem> adminList(@Param("status") String status);

    @Select("SELECT * FROM v_order_detail WHERE order_id = #{orderId}")
    List<OrderDetailRow> selectDetail(Integer orderId);

    @Update("UPDATE t_user SET points = points + #{points}, "
            + "level = CASE WHEN points >= 5000 THEN 'SVIP' WHEN points >= 1000 THEN 'VIP' ELSE '普通' END "
            + "WHERE user_id = #{userId}")
    void addPoints(@Param("userId") Integer userId, @Param("points") int points);
}

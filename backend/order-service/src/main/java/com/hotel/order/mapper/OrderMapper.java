package com.hotel.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hotel.order.entity.Order;
import com.hotel.order.entity.OrderDetailRow;
import com.hotel.order.entity.OrderListItem;
import com.hotel.order.entity.RoomInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    @Select("SELECT room_type_id, hotel_id, type_name, price, capacity, available_count "
            + "FROM t_room_type WHERE room_type_id = #{roomTypeId}")
    RoomInfo selectRoomInfo(Integer roomTypeId);

    @Select("SELECT level FROM t_user WHERE user_id = #{userId}")
    String selectUserLevel(Integer userId);

    /** 我的订单：按需求文档要求通过存储过程 sp_user_orders 查询 */
    List<OrderListItem> callUserOrders(@Param("userId") Integer userId);

    /** 后台订单列表（可按状态筛选） */
    List<OrderListItem> adminList(@Param("status") String status);

    /** 订单明细（视图 v_order_detail） */
    @Select("SELECT * FROM v_order_detail WHERE order_id = #{orderId}")
    List<OrderDetailRow> selectDetail(Integer orderId);

    /**
     * 订单完成后给用户加积分并按新积分升级：
     * MySQL 的 SET 从左到右生效，level 的 CASE 用的是加完之后的积分。
     */
    @Update("UPDATE t_user SET points = points + #{points}, "
            + "level = CASE WHEN points >= 5000 THEN 'SVIP' WHEN points >= 1000 THEN 'VIP' ELSE '普通' END "
            + "WHERE user_id = #{userId}")
    void addPoints(@Param("userId") Integer userId, @Param("points") int points);
}

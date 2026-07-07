package com.hotel.order.mapper;

import com.hotel.order.entity.OrderItem;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OrderItemMapper {

    @Insert("INSERT INTO t_order_item (order_id, room_type_id, price) "
            + "VALUES (#{orderId}, #{roomTypeId}, #{price})")
    @Options(useGeneratedKeys = true, keyProperty = "itemId")
    int insert(OrderItem item);

    @Insert("INSERT INTO t_item_guest (item_id, guest_id) VALUES (#{itemId}, #{guestId})")
    void insertItemGuest(@Param("itemId") Integer itemId, @Param("guestId") Integer guestId);
}

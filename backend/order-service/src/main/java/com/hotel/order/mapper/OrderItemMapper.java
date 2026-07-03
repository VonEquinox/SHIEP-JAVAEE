package com.hotel.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hotel.order.entity.OrderItem;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {

    @Insert("INSERT INTO t_item_guest (item_id, guest_id) VALUES (#{itemId}, #{guestId})")
    void insertItemGuest(@Param("itemId") Integer itemId, @Param("guestId") Integer guestId);
}

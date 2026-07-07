package com.hotel.order.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/** 订单详情：订单信息 + 每间房的入住人 */
@Data
public class OrderDetailVO {

    private OrderListItem order;
    private List<ItemVO> items;

    @Data
    public static class ItemVO {
        private Integer itemId;
        private String typeName;
        private BigDecimal roomPrice;
        private List<GuestVO> guests;
    }

    @Data
    public static class GuestVO {
        private String name;
        private String gender;
        private Integer age;
    }
}

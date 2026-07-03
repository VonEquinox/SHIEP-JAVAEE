package com.hotel.order.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/** 下单请求：一次可订多间、多房型，每间房需指定入住人 */
@Data
public class CreateOrderDTO {

    private Integer hotelId;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private List<RoomReq> rooms;

    @Data
    public static class RoomReq {
        private Integer roomTypeId;
        private List<Integer> guestIds;
    }
}

package com.hotel.hotel.controller;

import com.hotel.common.Result;
import com.hotel.hotel.entity.RoomType;
import com.hotel.hotel.service.HotelService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/room")
@RequiredArgsConstructor
public class RoomController {

    private final HotelService hotelService;

    /** 后台：新增/修改房型 */
    @PostMapping
    public Result<Void> save(@RequestHeader("X-User-Role") String role, @RequestBody RoomType room) {
        HotelController.checkAdmin(role);
        hotelService.saveRoomType(room);
        return Result.ok();
    }

    /** 后台：删除房型 */
    @DeleteMapping("/{roomTypeId}")
    public Result<Void> delete(@RequestHeader("X-User-Role") String role, @PathVariable Integer roomTypeId) {
        HotelController.checkAdmin(role);
        hotelService.deleteRoomType(roomTypeId);
        return Result.ok();
    }
}

package com.hotel.hotel.controller;

import com.hotel.common.Result;
import com.hotel.hotel.entity.Hotel;
import com.hotel.hotel.entity.HotelRoom;
import com.hotel.hotel.service.HotelService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/hotel")
@RequiredArgsConstructor
public class HotelController {

    private final HotelService hotelService;

    /** 组合条件搜索酒店（游客可用） */
    @GetMapping("/search")
    public Result<List<Hotel>> search(@RequestParam(required = false) String city,
                                      @RequestParam(required = false) String district,
                                      @RequestParam(required = false) String name,
                                      @RequestParam(required = false) BigDecimal priceMin,
                                      @RequestParam(required = false) BigDecimal priceMax) {
        return Result.ok(hotelService.search(city, district, name, priceMin, priceMax));
    }

    /** 酒店客房详情（视图 v_hotel_room） */
    @GetMapping("/{hotelId}/rooms")
    public Result<List<HotelRoom>> rooms(@PathVariable Integer hotelId) {
        return Result.ok(hotelService.rooms(hotelId));
    }

    /** 城市-区域 级联下拉数据 */
    @GetMapping("/cities")
    public Result<Map<String, List<String>>> cities() {
        return Result.ok(hotelService.cities());
    }

    /** 后台：新增/修改酒店 */
    @PostMapping
    public Result<Void> save(@RequestHeader("X-User-Role") String role, @RequestBody Hotel hotel) {
        checkAdmin(role);
        hotelService.saveHotel(hotel);
        return Result.ok();
    }

    static void checkAdmin(String role) {
        if (!"ADMIN".equals(role)) {
            throw new IllegalArgumentException("无权限");
        }
    }
}

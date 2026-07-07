package com.hotel.user.controller;

import com.hotel.common.Result;
import com.hotel.user.entity.Guest;
import com.hotel.user.service.GuestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/guest")
@RequiredArgsConstructor
public class GuestController {

    private final GuestService guestService;

    @GetMapping("/list")
    public Result<List<Guest>> list(@RequestHeader("X-User-Id") Integer userId) {
        return Result.ok(guestService.listByUser(userId));
    }

    @PostMapping
    public Result<Void> add(@RequestHeader("X-User-Id") Integer userId, @RequestBody Guest guest) {
        guestService.add(userId, guest);
        return Result.ok();
    }

    @PutMapping
    public Result<Void> update(@RequestHeader("X-User-Id") Integer userId, @RequestBody Guest guest) {
        guestService.update(userId, guest);
        return Result.ok();
    }

    @DeleteMapping("/{guestId}")
    public Result<Void> delete(@RequestHeader("X-User-Id") Integer userId, @PathVariable Integer guestId) {
        guestService.delete(userId, guestId);
        return Result.ok();
    }
}

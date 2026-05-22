package com.luqiang.seckill.controller;

import com.luqiang.seckill.common.ApiResponse;
import com.luqiang.seckill.entity.OrderInfo;
import com.luqiang.seckill.service.SeckillService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/seckill")
public class SeckillController {

    private final SeckillService seckillService;

    public SeckillController(SeckillService seckillService) {
        this.seckillService = seckillService;
    }

    @PostMapping("/do/{id}")
    public ApiResponse<Void> doSeckill(@PathVariable Long id,
                                       @RequestParam(required = false) String userId) {
        if (userId == null || userId.isBlank()) {
            return ApiResponse.fail(401, "userId 缺失");
        }
        return seckillService.executeSeckill(id, userId);
    }

    @GetMapping("/stock/{id}")
    public ApiResponse<Integer> getStock(@PathVariable Long id) {
        return seckillService.getStock(id);
    }

    @GetMapping("/result/{id}")
    public ApiResponse<OrderInfo> getResult(@PathVariable Long id,
                                            @RequestParam(required = false) String userId) {
        if (userId == null || userId.isBlank()) {
            return ApiResponse.fail(401, "userId 缺失");
        }
        return seckillService.getResult(id, userId);
    }
}
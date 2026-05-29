package com.luqiang.seckill.controller;

import com.luqiang.seckill.common.ApiResponse;
import com.luqiang.seckill.entity.OrderInfo;
import com.luqiang.seckill.interceptor.JwtAuthInterceptor;
import com.luqiang.seckill.service.SeckillService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/seckill")
public class SeckillController {

    private final SeckillService seckillService;

    public SeckillController(SeckillService seckillService) {
        this.seckillService = seckillService;
    }

    @PostMapping("/do/{id}")
    public ApiResponse<Void> doSeckill(@PathVariable Long id, HttpServletRequest request) {
        String userId = (String) request.getAttribute(JwtAuthInterceptor.USER_ID_ATTR);
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
    public ApiResponse<OrderInfo> getResult(@PathVariable Long id, HttpServletRequest request) {
        String userId = (String) request.getAttribute(JwtAuthInterceptor.USER_ID_ATTR);
        if (userId == null || userId.isBlank()) {
            return ApiResponse.fail(401, "userId 缺失");
        }
        return seckillService.getResult(id, userId);
    }

    @GetMapping("/orders/{id}")
    public ApiResponse<?> getRecentOrders(@PathVariable Long id,
                                          @RequestParam(defaultValue = "20") int limit) {
        return seckillService.getRecentOrders(id, limit);
    }

    @PostMapping("/pay/{id}")
    public ApiResponse<Void> payOrder(@PathVariable Long id, HttpServletRequest request) {
        String userId = (String) request.getAttribute(JwtAuthInterceptor.USER_ID_ATTR);
        if (userId == null || userId.isBlank()) {
            return ApiResponse.fail(401, "userId 缺失");
        }
        return seckillService.payOrder(id, userId);
    }

    @GetMapping("/my-orders")
    public ApiResponse<?> getMyOrders(HttpServletRequest request,
                                      @RequestParam(required = false) Integer status,
                                      @RequestParam(defaultValue = "50") int limit) {
        String userId = (String) request.getAttribute(JwtAuthInterceptor.USER_ID_ATTR);
        if (userId == null || userId.isBlank()) {
            return ApiResponse.fail(401, "userId 缺失");
        }
        return seckillService.getMyOrders(userId, status, limit);
    }
}

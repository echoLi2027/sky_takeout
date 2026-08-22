package com.sky.controller.admin;

import com.sky.result.Result;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/admin/shop")
@RestController("adminShopController")
@Api(tags = "shop relevant operations")
@Slf4j
public class ShopController {

    private static final String KEY = "SHOP_STATUS";

    @Autowired
    private RedisTemplate redisTemplate;

    @PutMapping("/{status}")
    public Result setShopStatus(@PathVariable Integer status){
        log.info("zzy_log: shop running status: {}", status == 1 ? "opening" : "closed");

        redisTemplate.opsForValue().set(KEY,status);

        return Result.success();
    }
}

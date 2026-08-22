package com.sky.controller.user;

import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/user/shop")
@RestController("userShopController")
@Api(tags = "shop relevant operations")
@Slf4j
public class ShopController {

    private static final String KEY = "SHOP_STATUS";

    @Autowired
    private RedisTemplate redisTemplate;

    @GetMapping("/{status}")
    @ApiOperation("get shop status")
    public Result getShopStatus(){

        Integer status = (Integer)redisTemplate.opsForValue().get(KEY);

        log.info("zzy_log: user get shop running status: {}", status == 1 ? "opening" : "closed");

        return Result.success(status);
    }
}

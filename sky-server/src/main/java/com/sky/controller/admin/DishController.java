package com.sky.controller.admin;

import com.sky.dto.DishDTO;
import com.sky.result.Result;
import com.sky.service.DishService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/admin/dish")
@RestController
@Api(tags = "dish operations")
@Slf4j
public class DishController {

    @Autowired
    private DishService dishService;

    @PostMapping
    @ApiOperation("create new dish")
    public Result addDish(@RequestBody DishDTO dishDTO){

        log.info("zzy_log: inserted dishDTO: {}",dishDTO);

        dishService.save(dishDTO);

        return Result.success();
    }
}

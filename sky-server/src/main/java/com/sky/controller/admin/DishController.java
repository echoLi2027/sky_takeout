package com.sky.controller.admin;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping("/page")
    @ApiOperation("query dish by page")
    public Result<PageResult> page(DishPageQueryDTO dishPageQueryDTO){

        log.info("zzy_log: page dishPageQueryDTO: {}",dishPageQueryDTO);

        PageResult result = dishService.pageQuery(dishPageQueryDTO);

        return Result.success(result);
    }

    @DeleteMapping
    @ApiOperation("delete dishes")
    public Result deleteIds(@RequestParam List<Long> ids){

        log.info("zzy_log: delete dishes ids: {}",ids);

        dishService.deleteIds(ids);

        return Result.success();
    }
}

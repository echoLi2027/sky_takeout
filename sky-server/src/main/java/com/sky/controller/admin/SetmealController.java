package com.sky.controller.admin;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Api(tags = "setmeal operations")
@RestController
@RequestMapping("/admin/setmeal")
public class SetmealController {

    @Autowired
    private SetmealService setmealService;

    @PostMapping
    @ApiOperation("insert setmeal")
    public Result insertSetMeal(@RequestBody SetmealDTO setmealDTO){
        log.info("zzy_log: insert set meal info: {}", setmealDTO);

        setmealService.insertSetmeal(setmealDTO);

        return Result.success();

    }

    @GetMapping("/page")
    @ApiOperation("query set meal by page")
    public Result<PageResult> queryPage(SetmealPageQueryDTO setmealPageQueryDTO){
        log.info("zzy_log: page query set meal info: {}", setmealPageQueryDTO);

        PageResult result = setmealService.pageQuery(setmealPageQueryDTO);

        return Result.success(result);

    }
}

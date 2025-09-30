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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Api(tags = "setmeal operations")
@RestController
@RequestMapping("/admin/setmeal")
public class SetmealController {

    @Autowired
    private SetmealService setmealService;

    @PostMapping
    @ApiOperation("insert setmeal")
    @CacheEvict(cacheNames = "setmealCache", key = "#setmealDTO.categoryId")
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

    @DeleteMapping
    @ApiOperation(("batch delete set meals"))
    @CacheEvict(cacheNames = "setmealCache", allEntries = true)
    public Result deleteSetmeals(@RequestParam List<Long> ids){
        log.info("zzy_log: delete set meal ids: {}", ids);

        setmealService.deleteSetmeals(ids);

        return Result.success();
    }

    @PostMapping("/status/{status}")
    @ApiOperation("enable/disable setmeal status")
    @CacheEvict(cacheNames = "setmealCache", allEntries = true)
    public Result setStatus(@PathVariable Integer status, Long id){
        log.info("zzy_log: set setmeal status which setmeal need to change what status and setmealId: {}, {}", status,id);

        setmealService.setStatus(status,id);

        return Result.success();
    }

    @GetMapping("/{id}")
    @ApiOperation("get setmeal info by id")
    public Result<SetmealVO> getById(@PathVariable Long id){
        log.info("zzy_log: search by setmeal id: {}", id);

        SetmealVO setmealVO = setmealService.getById(id);

        return Result.success(setmealVO);
    }

    @PutMapping
    @ApiOperation("update setmeal info")
    @CacheEvict(cacheNames = "setmealCache", allEntries = true)
    public Result updateSetmeal(@RequestBody SetmealDTO setmealDTO){
//        no setmealId
        log.info("zzy_log: update setmeal info: {}", setmealDTO);

        setmealService.update(setmealDTO);

        return Result.success();
    }
}

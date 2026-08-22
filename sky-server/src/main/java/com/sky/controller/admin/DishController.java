package com.sky.controller.admin;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RequestMapping("/admin/dish")
@RestController
@Api(tags = "dish operations")
@Slf4j
public class DishController {

    @Autowired
    private DishService dishService;

    @Autowired
    private RedisTemplate redisTemplate;

    @GetMapping("/list")
    @ApiOperation("get all dishes info")
    public Result<List<Dish>> searchByCategory(Long categoryId){

        log.info("search dishes by categoryId: {}",categoryId);

        List<Dish> dishes = dishService.getCategoryDish(categoryId);

        return Result.success(dishes);
    }

    @PostMapping
    @ApiOperation("create new dish")
    public Result addDish(@RequestBody DishDTO dishDTO){

        log.info("zzy_log: inserted dishDTO: {}",dishDTO);

        dishService.save(dishDTO);

//        if we add new dishes we need to delete that dish's category in redis
        String key = "dish_" + dishDTO.getCategoryId();
        cleanCache(key);

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

//        when we delete dishes we need to load the whole data again to the frontend
//        because it consumes more time to search to decided which redis should be deleted.
//        and also be aware here is not simply toString, but using * to match all scenarios
        cleanCache("dish_*");

        return Result.success();
    }

    @GetMapping("/{id}")
    @ApiOperation("get dish info for editing dish")
    public Result<DishVO> searchById(@PathVariable Long id){

        log.info("zzy_log: search dishes id: {}",id);

        DishVO dishVO = dishService.selectDishById(id);

        return Result.success(dishVO);
    }

    /**
     * 菜品起售停售
     *
     * @param status
     * @param id
     * @return
     */
    @PostMapping("/status/{status}")
    @ApiOperation("菜品起售停售")
    public Result<String> startOrStop(@PathVariable Integer status, Long id) {
        dishService.startOrStop(status, id);

        //将所有的菜品缓存数据清理掉，所有以dish_开头的key
        cleanCache("dish_*");

        return Result.success();
    }

    @PutMapping
    @ApiOperation("edit dish info")
    public Result updateDish(@RequestBody DishDTO dishDTO){

        log.info("zzy_log: edit dishes info: {}",dishDTO);

        dishService.updateDish(dishDTO);

        cleanCache("dish_*");

        return Result.success();

    }

    /**
     * delete cache in redis according to patterns
     * could be single field in a key could be the whole key
     * @param pattern
     */
    private void cleanCache(String pattern){
        Set keys = redisTemplate.keys(pattern);
        redisTemplate.delete(keys);
    }
}

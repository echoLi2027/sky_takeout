package com.sky.service;

import com.sky.dto.CategoryDTO;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Category;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.vo.DishVO;

import java.util.List;

public interface DishService {

    /**
     * add new dish
     * @param dishDTO
     */
    void save(DishDTO dishDTO);

    /**
     * page query dishes on requirements
     * @param dishPageQueryDTO
     * @return
     */
    PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO);

    /**
     * delete dishes
     * @param ids
     */
    void deleteIds(List<Long> ids);

    /**
     * query dish by id
     * @param id
     * @return
     */
    DishVO selectDishById(Long id);

    /**
     * update dish and dish_flavor info
     * @param dishDTO
     */
    void updateDish(DishDTO dishDTO);


    /**
     * query all dishes by category
     * @return
     */
    List<Dish> getCategoryDish(Long categoryId);


    /**
     * 条件查询菜品和口味
     * @param dish
     * @return
     */
    List<DishVO> listWithFlavor(Dish dish);

    void startOrStop(Integer status, Long id);
}

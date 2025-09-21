package com.sky.service;

import com.sky.dto.CategoryDTO;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.dto.DishDTO;
import com.sky.entity.Category;
import com.sky.result.PageResult;

import java.util.List;

public interface DishService {

    /**
     * add new dish
     * @param dishDTO
     */
    void save(DishDTO dishDTO);

}

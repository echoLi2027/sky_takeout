package com.sky.service;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.vo.SetmealVO;

import java.util.List;

public interface SetmealService {

    /**
     * insert set meal
     * @param setmealDTO
     */
    void insertSetmeal(SetmealDTO setmealDTO);

    /**
     * query set meal by page
     * @param setmealPageQueryDTO
     * @return
     */
    PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO);

    /**
     * delete set meals and respective set_meal_dishes
     * @param ids
     */
    void deleteSetmeals(List<Long> ids);
}

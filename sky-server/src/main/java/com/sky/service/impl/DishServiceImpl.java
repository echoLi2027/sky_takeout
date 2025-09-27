package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.annotation.AutoFill;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.CategoryDTO;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Category;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.entity.Setmeal;
import com.sky.enumeration.OperationType;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.exception.SetmealEnableFailedException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.CategoryService;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 分类业务层
 */
@Service
@Slf4j
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DishFlavorMapper dishFlavorMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;



    @Transactional(rollbackFor = RuntimeException.class)
    @Override
    public void save(DishDTO dishDTO) {

        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO,dish);

        dishMapper.insert(dish);

        Long dishId = dish.getId();

        List<DishFlavor> flavors = dishDTO.getFlavors();

        if (flavors != null && flavors.size()>0){
            flavors.forEach(flavor -> {flavor.setDishId(dishId);});
            dishFlavorMapper.saveBatch(flavors);
        }

    }

    /**
     * page query dishes on requirements
     * @param dishPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {

        PageHelper.startPage(dishPageQueryDTO.getPage(),dishPageQueryDTO.getPageSize());


        Page<DishVO> pageData = dishMapper.page(dishPageQueryDTO);


        return new PageResult(pageData.getTotal(), pageData.getResult());
    }


    @Transactional(rollbackFor = RuntimeException.class)
    @Override
    public void deleteIds(List<Long> ids) {
/*
//***********logic flaw***********
        for (Long id : ids) {
//         1. setmeal_dish doesn't have current deleted dish then we can delete the dish
            Setmeal setmeal = setmealDishMapper.selectByDishId(id);
            if (setmeal != null){
                throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
            }else{
                //        2. if dish is not involved in the set meal, delete the flavor connected with the dish
                dishFlavorMapper.deleteByDishId(id);
                //        3.delete dish
                dishMapper.deleteById(id);
            }

        }

        //***********logic flaw***********
 */


//        1. check dish status, if it's enable then cannot delete
        for (Long id : ids) {
            DishVO dishVO = dishMapper.selectById(id);
            if (dishVO.getStatus() == StatusConstant.ENABLE){
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }


//        2. check all dishes' setmeal_dish condition, find out whether dishes bind with set meal
//        if bind with set meal cannot delete
        List<Long> setmeal_ids = setmealDishMapper.selectByDishIds(ids);
        if (setmeal_ids != null && setmeal_ids.size() > 0){
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }


//        3. everything checked, now can delete
        for (Long id : ids) {
            //        3.1. if dish is not involved in the set meal, delete the flavor connected with the dish
            dishFlavorMapper.deleteByDishId(id);
            //        3.2. delete dish
            dishMapper.deleteById(id);

        }


    }

    @Override
    public DishVO selectDishById(Long dishId) {

        List<DishFlavor> flavors = dishFlavorMapper.selectByDishId(dishId);

        DishVO dishVO = dishMapper.selectById(dishId);

        dishVO.setFlavors(flavors);

        return dishVO;
    }

    @Override
    public void updateDish(DishDTO dishDTO) {

//        1. update dish flavor
/*
// not good, this way need to call sql 2 times
//        1.1 delete, if dish flavor used to not null
        DishVO dishVO = dishMapper.selectById(dishDTO.getId());
        if (dishVO.getFlavors() != null && dishVO.getFlavors().size() > 0){
            dishFlavorMapper.deleteByDishId(dishDTO.getId());
        }*/

//        1.1 delete dish flavor
        dishFlavorMapper.deleteByDishId(dishDTO.getId());
//        1.2 insert, if dishDTO has dish flavor info
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors!=null && flavors.size()>0){
//            don't forget set dish_id, cause there is no dish_id in dishDTO
            flavors.forEach(dishFlavor -> dishFlavor.setDishId(dishDTO.getId()));
            dishFlavorMapper.saveBatch(flavors);
        }

//        2. update dish info
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO,dish);
        dishMapper.update(dish);
    }

    @Override
    public List<Dish> getCategoryDish(Long categoryId) {
        return dishMapper.selectByCategoryId(categoryId);
    }


    /**
     * 条件查询菜品和口味
     * @param dish
     * @return
     */
    public List<DishVO> listWithFlavor(Dish dish) {

//        List<Dish> dishList = dishMapper.list(dish);
        List<Dish> dishList = dishMapper.selectByCategoryId(dish.getCategoryId());

        List<DishVO> dishVOList = new ArrayList<>();

        for (Dish d : dishList) {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(d,dishVO);

            //根据菜品id查询对应的口味
            List<DishFlavor> flavors = dishFlavorMapper.selectByDishId(d.getId());

            dishVO.setFlavors(flavors);
            dishVOList.add(dishVO);
        }

        return dishVOList;
    }

}

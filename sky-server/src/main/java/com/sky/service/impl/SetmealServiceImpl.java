package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.exception.SetmealEnableFailedException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.DishVO;
import com.sky.vo.SetmealVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class SetmealServiceImpl implements SetmealService {

    @Autowired
    private SetmealMapper setmealMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;

    @Autowired
    private DishMapper dishMapper;

    @Transactional(rollbackFor = RuntimeException.class)
    @Override
    public void insertSetmeal(SetmealDTO setmealDTO) {

//        1. insert dish, then get the setmealId then we need to assigned to setmealDish
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO,setmeal);
        setmealMapper.insert(setmeal);

        Long setmealId = setmeal.getId();

//        2. insert setmeal_dish
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        setmealDishes.forEach(setmealDish -> {setmealDish.setSetmealId(setmealId);});
        setmealDishMapper.insertBatch(setmealDishes);


    }

    @Override
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {

        PageHelper.startPage(setmealPageQueryDTO.getPage(),setmealPageQueryDTO.getPageSize());

        Page<SetmealVO> page = setmealMapper.pageQuery(setmealPageQueryDTO);


        return new PageResult(page.getTotal(),page.getResult());
    }

    @Transactional(rollbackFor = RuntimeException.class)
    @Override
    public void deleteSetmeals(List<Long> ids) {
//        1. if status = 1 not allowed to delete
        for (Long id : ids) {
            Setmeal setmeal = setmealMapper.queryById(id);
            if (setmeal.getStatus() == 1){
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        }


//        2. delete respective setmeal_dishes
        setmealDishMapper.deleteBySetmealIds(ids);

//        3. delete setmeals
        setmealMapper.deleteByIds(ids);

    }

    @Override
    public void setStatus(Integer status, Long id) {

//        1. if it's enable setmeal, check the status of dishes under setmeal
        if (status.equals(StatusConstant.ENABLE)){
            List<Long> dishIds = setmealDishMapper.selectBySetmealId(Collections.singletonList(id));
            for (Long dishId : dishIds) {
                Integer dish_status = dishMapper.selectById(dishId).getStatus();
                if (dish_status.equals(StatusConstant.DISABLE)){
                    throw new SetmealEnableFailedException(MessageConstant.SETMEAL_ENABLE_FAILED);
                }
            }
        }

//        2. set status
        Setmeal setmeal = Setmeal.builder()
                .status(status)
                .id(id)
                .build();

        setmealMapper.update(setmeal);

    }

    @Transactional(rollbackFor = RuntimeException.class)
    @Override
    public SetmealVO getById(Long setmealId) {

        Setmeal setmeal = setmealMapper.queryById(setmealId);

        List<SetmealDish> setmealDishList = setmealDishMapper.selectAllBySetmealId(setmealId);

        SetmealVO setmealVO = new SetmealVO();

        BeanUtils.copyProperties(setmeal,setmealVO);
        setmealVO.setSetmealDishes(setmealDishList);

        return setmealVO;
    }

    @Transactional(rollbackFor = RuntimeException.class)
    @Override
    public void update(SetmealDTO setmealDTO) {
//        1. update setmeal_dish
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        setmealDishes.forEach(setmealDish -> {setmealDish.setSetmealId(setmealDTO.getId());});
//        1.1 delete previous record related to this setmealId
        setmealDishMapper.deleteBySetmealIds(Collections.singletonList(setmealDTO.getId()));

//        1.2 insert batched setmeal_dish
        setmealDishMapper.insertBatch(setmealDishes);

//        2. update setmeal
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO,setmeal);
        setmealMapper.update(setmeal);

    }
}

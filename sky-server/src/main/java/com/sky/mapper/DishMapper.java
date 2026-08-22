package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.annotation.AutoFill;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.enumeration.OperationType;
import com.sky.vo.DishVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface DishMapper {

    /**
     * 根据分类id查询菜品数量
     * @param categoryId
     * @return
     */
    @Select("select count(id) from dish where category_id = #{categoryId}")
    Integer countByCategoryId(Long categoryId);

    /**
     * insert dish
     * @param dish
     * @return
     */
    @AutoFill(OperationType.INSERT)
    Integer insert(Dish dish);


    Page<DishVO> page(DishPageQueryDTO dishPageQueryDTO);

    @Delete("delete from dish where id = #{id}")
    void deleteById(Long id);

    @Select("select * from dish where id = #{id}")
    DishVO selectById(Long id);

    @AutoFill(OperationType.UPDATE)
    void update(Dish dish);


    /**
     * select by category id, make sure that the dish is on sell
     * @param categoryId
     * @return
     */
    @Select("select * from dish where category_id = #{categoryID} and status = 1")
    List<Dish> selectByCategoryId(Long categoryId);

    /**
     * 根据条件统计菜品数量
     * @param map
     * @return
     */
    Integer countByMap(Map map);
}

package com.sky.mapper;

import com.sky.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Mapper
public interface UserMapper {

    @Select("select * from user where openid = #{openid}")
    User getByopenId(String openid);


    void insert(User user);

    @Select("select * from user where id = #{id}")
    User getById(Long id);

    /**
     * get user count
     * @param begin
     * @param end
     */
    Integer userStatistics(LocalDateTime begin, LocalDateTime end);

    Integer countByMap(Map map);
}

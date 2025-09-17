package com.sky.service;

import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.result.PageResult;

public interface EmployeeService {

    /**
     * 员工登录
     * @param employeeLoginDTO
     * @return
     */
    Employee login(EmployeeLoginDTO employeeLoginDTO);

    /**
     * insert emp into db
     * @param employeeDTO
     */
    void saveEmp(EmployeeDTO employeeDTO);

    /**
     * search emp by name(if has) and display by page
     * @param employeePageQueryDTO
     * @return
     */
    PageResult search(EmployeePageQueryDTO employeePageQueryDTO);

    /**
     * set emp status: disable or enable
     * @param status
     */
    void setStatus(Integer status, Long id);

    /**
     * search emp by id
     * @param id
     * @return
     */
    Employee searchById(Integer id);

    /**
     * edit emp
     * @param employeeDTO
     */
    void editEmp(EmployeeDTO employeeDTO);
}

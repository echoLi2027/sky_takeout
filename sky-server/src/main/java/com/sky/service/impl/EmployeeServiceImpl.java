package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.PasswordConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.exception.AccountLockedException;
import com.sky.exception.AccountNotFoundException;
import com.sky.exception.PasswordErrorException;
import com.sky.mapper.EmployeeMapper;
import com.sky.result.PageResult;
import com.sky.service.EmployeeService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    @Autowired
    private EmployeeMapper employeeMapper;

    /**
     * 员工登录
     *
     * @param employeeLoginDTO
     * @return
     */
    public Employee login(EmployeeLoginDTO employeeLoginDTO) {
        String username = employeeLoginDTO.getUsername();
        String password = employeeLoginDTO.getPassword();

        //1、根据用户名查询数据库中的数据
        Employee employee = employeeMapper.getByUsername(username);

        //2、处理各种异常情况（用户名不存在、密码不对、账号被锁定）
        if (employee == null) {
            //账号不存在
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        //密码比对
        // md5 cipher，then contrast
        password = DigestUtils.md5DigestAsHex(password.getBytes());
        if (!password.equals(employee.getPassword())) {
            //密码错误
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }

        if (employee.getStatus() == StatusConstant.DISABLE) {
            //账号被锁定
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
        }

        //3、返回实体对象
        return employee;
    }

    @Override
    public void saveEmp(EmployeeDTO employeeDTO) {

        Employee employee = new Employee();

//        copy attrs in employeeDTO into employee obj
        BeanUtils.copyProperties(employeeDTO,employee);

        employee.setStatus(StatusConstant.ENABLE);


//        cipher the initial password
        employee.setPassword(DigestUtils.md5DigestAsHex(PasswordConstant.DEFAULT_PASSWORD.getBytes()));

        /*
        employee.setCreateTime(LocalDateTime.now());
        employee.setUpdateTime(LocalDateTime.now());

//        from JwtTokenAdminInterceptor saved emp_id get current id => actually is threadLocal
        employee.setCreateUser(BaseContext.getCurrentId());
        employee.setUpdateUser(BaseContext.getCurrentId());

*/
        employeeMapper.insert(employee);


    }

    /**
     * display emp info by page
     * @param pageQueryDTO
     */
    @Override
    public PageResult search(EmployeePageQueryDTO pageQueryDTO) {

        PageHelper.startPage(pageQueryDTO.getPage(), pageQueryDTO.getPageSize());

        Page<Employee> employees = (Page)employeeMapper.queryPage(pageQueryDTO);

        return new PageResult(employees.getTotal(),employees.getResult());

    }

    /**
     * set emp status: disable or enable
     * @param status
     */
    @Override
    public void setStatus(Integer status, Long id) {

        Employee employee = Employee.builder()
                .id(id)
                .status(status)
                .build();

        employeeMapper.updateEmp(employee);

    }

    @Override
    public Employee searchById(Integer id) {

        Employee employee = employeeMapper.searchById(id);
//        protect user info, so that the actual pw won't pass to the browser
        employee.setPassword("******");

        return employee;
    }

    @Override
    public void editEmp(EmployeeDTO employeeDTO) {
        Employee employee = new Employee();

        BeanUtils.copyProperties(employeeDTO, employee);

//        employee.setUpdateUser(BaseContext.getCurrentId());
//
//        employee.setUpdateTime(LocalDateTime.now());

        employeeMapper.updateEmp(employee);

    }

}

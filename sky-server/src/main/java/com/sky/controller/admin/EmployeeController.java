package com.sky.controller.admin;

import com.sky.constant.JwtClaimsConstant;
import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.properties.JwtProperties;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.EmployeeService;
import com.sky.utils.JwtUtil;
import com.sky.vo.EmployeeLoginVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 员工管理
 */
@RestController
@RequestMapping("/admin/employee")
@Slf4j
@Api(tags = "employee interface")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;
    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 登录
     *
     * @param employeeLoginDTO
     * @return
     */
    @PostMapping("/login")
    @ApiOperation(value = "emp login")
    public Result<EmployeeLoginVO> login(@RequestBody EmployeeLoginDTO employeeLoginDTO) {
        log.info("employee login：{}", employeeLoginDTO);

        Employee employee = employeeService.login(employeeLoginDTO);

//        after login success, generate jwt token
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.EMP_ID, employee.getId());
        String token = JwtUtil.createJWT(
                jwtProperties.getAdminSecretKey(),
                jwtProperties.getAdminTtl(),
                claims);

//        Immutability - Once built, the object can't be changed (safer in multi-thread environments)
        EmployeeLoginVO employeeLoginVO = EmployeeLoginVO.builder()
                .id(employee.getId())
                .userName(employee.getUsername())
                .name(employee.getName())
                .token(token)
                .build();

        return Result.success(employeeLoginVO);
    }

    /**
     * insert emp
     * @param employeeDTO
     * @return
     */
    @PostMapping
    @ApiOperation("insert emp")
    public Result insertEmp(@RequestBody EmployeeDTO employeeDTO){

        log.info("zzy_log: controller, inserted emp info: {}", employeeDTO);

        employeeService.saveEmp(employeeDTO);


        return Result.success();
    }

    /**
     * search emp by name(if has) and display by page
     * @param employeePageQueryDTO
     * @return
     */
    @GetMapping("/page")
    @ApiOperation("paging displaying emp")
    public Result empPage(EmployeePageQueryDTO employeePageQueryDTO){
        log.info("zzy_log: paging emp request info : {}", employeePageQueryDTO);

        PageResult result = employeeService.search(employeePageQueryDTO);

        log.info("zzy_log: paging emp result info : {}", result);

        return Result.success(result);
    }

    /**
     * set emp status, disable or enable
     * @param status
     * @return
     */
    @PostMapping("/status/{status}")
    @ApiOperation("change emp status")
    public Result empStatus(@PathVariable Integer status, Long id){
        log.info("set emp status and emp_id: {}, {}", status,id);

        employeeService.setStatus(status, id);

        return Result.success();
    }

    @GetMapping("/{id}")
    @ApiOperation("search emp by id")
    public Result<Employee> empById(@PathVariable Integer id){
        log.info("get emp by id: {}",id);

        Employee employee = employeeService.searchById(id);

        return Result.success(employee);
    }

    @PutMapping
    @ApiOperation("edit emp info")
    public Result editEmp(@RequestBody EmployeeDTO employeeDTO){
        log.info("zzy_log: controller, edit emp info: {}", employeeDTO);

        employeeService.editEmp(employeeDTO);

        return Result.success();

    }

    /**
     * logout
     *
     * @return
     */
    @PostMapping("/logout")
    @ApiOperation(value = "emp logout")
    public Result<String> logout() {
        return Result.success();
    }

}

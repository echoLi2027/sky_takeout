package com.sky.e2e;

import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.entity.Employee;
import com.sky.result.Result;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EmployeeE2ETest extends BaseE2ETest {

    @Test
    void login_withRealCredentials_returnsGenuineToken() {
        seedEmployee("realuser", "mypassword", 1);

        EmployeeLoginDTO dto = new EmployeeLoginDTO();
        dto.setUsername("realuser");
        dto.setPassword("mypassword");

        ResponseEntity<Map> response = restTemplate.postForEntity("/admin/employee/login", dto, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map body = response.getBody();
        assertThat(body.get("code")).isEqualTo(1);
        Map data = (Map) body.get("data");
        assertThat(data.get("userName")).isEqualTo("realuser");
        assertThat((String) data.get("token")).isNotBlank();
    }

    @Test
    void login_wrongPassword_returnsPasswordError() {
        seedEmployee("pwuser", "correctpw", 1);

        EmployeeLoginDTO dto = new EmployeeLoginDTO();
        dto.setUsername("pwuser");
        dto.setPassword("wrongpw");

        ResponseEntity<Result> response = restTemplate.postForEntity("/admin/employee/login", dto, Result.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getCode()).isEqualTo(0);
        assertThat(response.getBody().getMsg()).isEqualTo("password error");
    }

    @Test
    void login_unknownUsername_returnsAccountNotFound() {
        EmployeeLoginDTO dto = new EmployeeLoginDTO();
        dto.setUsername("nosuchuser");
        dto.setPassword("whatever");

        ResponseEntity<Result> response = restTemplate.postForEntity("/admin/employee/login", dto, Result.class);

        assertThat(response.getBody().getCode()).isEqualTo(0);
        assertThat(response.getBody().getMsg()).isEqualTo("account not found");
    }

    @Test
    void insertEmp_withValidToken_persistsAndAutoFillsAuditFields() {
        Long operatorId = seedEmployee("operator1", "123456", 1);
        String token = adminToken(operatorId);

        EmployeeDTO dto = new EmployeeDTO();
        dto.setUsername("newhire");
        dto.setName("New Hire");
        dto.setPhone("13900000000");
        dto.setSex("1");
        dto.setIdNumber("110101199512121234");

        HttpEntity<EmployeeDTO> entity = new HttpEntity<>(dto, authHeaders(token));
        ResponseEntity<Result> response = restTemplate.exchange("/admin/employee", HttpMethod.POST, entity, Result.class);

        assertThat(response.getBody().getCode()).isEqualTo(1);

        Employee inserted = jdbcTemplate.queryForObject(
                "select * from employee where username = ?",
                (rs, rowNum) -> {
                    Employee e = new Employee();
                    e.setId(rs.getLong("id"));
                    e.setCreateUser(rs.getLong("create_user"));
                    e.setUpdateUser(rs.getLong("update_user"));
                    e.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
                    e.setUpdateTime(rs.getTimestamp("update_time").toLocalDateTime());
                    return e;
                }, "newhire");

        assertThat(inserted.getCreateUser()).isEqualTo(operatorId);
        assertThat(inserted.getUpdateUser()).isEqualTo(operatorId);
        assertThat(inserted.getCreateTime()).isAfter(LocalDateTime.now().minusMinutes(1));
        assertThat(inserted.getUpdateTime()).isAfter(LocalDateTime.now().minusMinutes(1));
    }

    @Test
    void insertEmp_withoutToken_isRejected() {
        EmployeeDTO dto = new EmployeeDTO();
        dto.setUsername("noauth");
        dto.setName("No Auth");

        HttpEntity<EmployeeDTO> entity = new HttpEntity<>(dto);
        ResponseEntity<String> response = restTemplate.exchange("/admin/employee", HttpMethod.POST, entity, String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void empPage_withToken_returnsSeededEmployees() {
        Long operatorId = seedEmployee("pageop", "123456", 1);
        seedEmployee("page.alice", "123456", 1);
        seedEmployee("page.bob", "123456", 1);
        String token = adminToken(operatorId);

        ResponseEntity<Map> response = restTemplate.exchange(
                "/admin/employee/page?page=1&pageSize=10",
                HttpMethod.GET, new HttpEntity<>(authHeaders(token)), Map.class);

        assertThat(response.getBody().get("code")).isEqualTo(1);
        Map data = (Map) response.getBody().get("data");
        assertThat(((Number) data.get("total")).intValue()).isEqualTo(3);
    }

    @Test
    void empStatus_disablesEmployee() {
        Long operatorId = seedEmployee("statusop", "123456", 1);
        Long targetId = seedEmployee("target", "123456", 1);
        String token = adminToken(operatorId);

        ResponseEntity<Result> response = restTemplate.exchange(
                "/admin/employee/status/0?id=" + targetId,
                HttpMethod.POST, new HttpEntity<>(authHeaders(token)), Result.class);

        assertThat(response.getBody().getCode()).isEqualTo(1);

        Integer status = jdbcTemplate.queryForObject("select status from employee where id = ?", Integer.class, targetId);
        assertThat(status).isEqualTo(0);
    }

    @Test
    void empById_masksPassword() {
        Long operatorId = seedEmployee("byidop", "123456", 1);
        Long targetId = seedEmployee("byidtarget", "secretpw", 1);
        String token = adminToken(operatorId);

        ResponseEntity<Map> response = restTemplate.exchange(
                "/admin/employee/" + targetId,
                HttpMethod.GET, new HttpEntity<>(authHeaders(token)), Map.class);

        Map data = (Map) response.getBody().get("data");
        assertThat(data.get("password")).isEqualTo("******");
    }

    @Test
    void editEmp_updatesNameAndAutoFillsUpdateUser() {
        Long operatorId = seedEmployee("editop", "123456", 1);
        Long targetId = seedEmployee("edittarget", "123456", 1);
        String token = adminToken(operatorId);

        EmployeeDTO dto = new EmployeeDTO();
        dto.setId(targetId);
        dto.setUsername("edittarget");
        dto.setName("Updated Name");

        HttpEntity<EmployeeDTO> entity = new HttpEntity<>(dto, authHeaders(token));
        ResponseEntity<Result> response = restTemplate.exchange("/admin/employee", HttpMethod.PUT, entity, Result.class);

        assertThat(response.getBody().getCode()).isEqualTo(1);

        Map<String, Object> row = jdbcTemplate.queryForMap("select name, update_user from employee where id = ?", targetId);
        assertThat(row.get("name")).isEqualTo("Updated Name");
        assertThat(((Number) row.get("update_user")).longValue()).isEqualTo(operatorId);
    }

    @Test
    void logout_withToken_succeeds() {
        Long operatorId = seedEmployee("logoutop", "123456", 1);
        String token = adminToken(operatorId);

        ResponseEntity<Result> response = restTemplate.exchange(
                "/admin/employee/logout", HttpMethod.POST, new HttpEntity<>(authHeaders(token)), Result.class);

        assertThat(response.getBody().getCode()).isEqualTo(1);
    }
}

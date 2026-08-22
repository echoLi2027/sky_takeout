package com.sky.e2e;

import com.sky.dto.EmployeeDTO;
import com.sky.result.Result;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerE2ETest extends BaseE2ETest {

    private String tokenAsOperator() {
        Long operatorId = seedEmployee("gehop", "123456", 1);
        return adminToken(operatorId);
    }

    @Test
    void baseException_categoryReferencedByDish_returnsResultErrorEnvelope() {
        String token = tokenAsOperator();
        Long categoryId = seedCategory("Referenced Cat", 1, 1);
        seedDish("Blocking Dish", categoryId, 0);

        ResponseEntity<Result> response = restTemplate.exchange(
                "/admin/category?id=" + categoryId, HttpMethod.DELETE, new HttpEntity<>(authHeaders(token)), Result.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getCode()).isEqualTo(0);
        assertThat(response.getBody().getMsg()).isEqualTo("当前分类关联了菜品,不能删除");
    }

    @Test
    void duplicateEmployeeUsername_currentBehavior() {
        String token = tokenAsOperator();

        EmployeeDTO first = new EmployeeDTO();
        first.setUsername("dupuser");
        first.setName("First");
        HttpEntity<EmployeeDTO> firstEntity = new HttpEntity<>(first, authHeaders(token));
        ResponseEntity<Result> firstResponse = restTemplate.exchange("/admin/employee", HttpMethod.POST, firstEntity, Result.class);
        assertThat(firstResponse.getBody().getCode()).isEqualTo(1);

        EmployeeDTO second = new EmployeeDTO();
        second.setUsername("dupuser");
        second.setName("Second");
        HttpEntity<EmployeeDTO> secondEntity = new HttpEntity<>(second, authHeaders(token));
        // The raw java.sql.SQLIntegrityConstraintViolationException from the unique-key
        // violation propagates uncaught through MyBatis and is caught by
        // GlobalExceptionHandler's dedicated handler for that exact type, which returns a
        // normal Result.error(...) envelope at HTTP 200 (no @ResponseStatus override) --
        // this path works as designed, unlike the OSS-upload and malformed-JSON gaps
        // documented elsewhere in this suite.
        ResponseEntity<Result> secondResponse = restTemplate.exchange("/admin/employee", HttpMethod.POST, secondEntity, Result.class);

        assertThat(secondResponse.getStatusCode().value()).isEqualTo(200);
        assertThat(secondResponse.getBody().getCode()).isEqualTo(0);
        assertThat(secondResponse.getBody().getMsg()).contains("dupuser").contains("already exist");
    }

    @Test
    void malformedJsonBody_fallsThroughToDefaultErrorResponse() {
        String token = tokenAsOperator();
        HttpHeaders headers = authHeaders(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>("{not valid json", headers);

        ResponseEntity<String> response = restTemplate.exchange("/admin/category", HttpMethod.POST, entity, String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).doesNotContain("\"code\"");
    }
}

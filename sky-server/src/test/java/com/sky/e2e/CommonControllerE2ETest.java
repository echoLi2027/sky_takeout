package com.sky.e2e;

import com.sky.result.Result;
import com.sky.utils.AliOssUtil;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CommonController.upload calls real Aliyun OSS -- mock it out here rather than hitting
 * the real cloud from tests.
 */
class CommonControllerE2ETest extends BaseE2ETest {

    @MockBean
    private AliOssUtil aliOssUtil;

    private String tokenAsOperator() {
        Long operatorId = seedEmployee("uploadop", "123456", 1);
        return adminToken(operatorId);
    }

    @Test
    void upload_withMockedOss_returnsFakeUrl() {
        String token = tokenAsOperator();
        Mockito.when(aliOssUtil.upload(Mockito.any(byte[].class), Mockito.anyString()))
                .thenReturn("https://fake-bucket.example.com/2026-08/fake.png");

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource("fake-image-bytes".getBytes()) {
            @Override
            public String getFilename() {
                return "photo.png";
            }
        });

        HttpHeaders headers = authHeaders(token);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<Result> response = restTemplate.exchange(
                "/admin/common/upload", HttpMethod.POST, entity, Result.class);

        assertThat(response.getBody().getCode()).isEqualTo(1);
        assertThat(response.getBody().getData()).isEqualTo("https://fake-bucket.example.com/2026-08/fake.png");
    }

    @Test
    void upload_withNoFileExtension_currentlyReturns500NotResultEnvelope() {
        String token = tokenAsOperator();

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource("fake-image-bytes".getBytes()) {
            @Override
            public String getFilename() {
                return "noextension";
            }
        });

        HttpHeaders headers = authHeaders(token);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

        // Documents current (buggy) behavior: CommonController#upload only catches
        // IOException, so the StringIndexOutOfBoundsException thrown by
        // originalFilename.substring(originalFilename.lastIndexOf(".")) for a
        // filename with no "." propagates uncaught past GlobalExceptionHandler (which
        // has no handler for it) and falls through to Spring Boot's default error
        // response instead of the app's Result envelope.
        ResponseEntity<String> response = restTemplate.exchange(
                "/admin/common/upload", HttpMethod.POST, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).doesNotContain("\"code\"");
    }
}

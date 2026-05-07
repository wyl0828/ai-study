package com.interview.coach.integration.piston;

import com.interview.coach.config.PistonProperties;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
@RequiredArgsConstructor
public class PistonClient {

    private final PistonProperties properties;

    public PistonExecuteResponse executeJava(String code, String stdin) {
        PistonExecuteRequest request = new PistonExecuteRequest();
        request.setLanguage("java");
        request.setVersion(properties.getJavaVersion());
        request.setFiles(List.of(new PistonFile("Main.java", code)));
        request.setStdin(stdin);
        request.setCompileTimeout(properties.getCompileTimeoutMs());
        request.setRunTimeout(properties.getRunTimeoutMs());
        URI executeUri = URI.create(normalizeBaseUrl(properties.getBaseUrl()) + "/execute");

        try {
            RestClient.RequestBodySpec spec = RestClient.builder()
                    .requestFactory(new JdkClientHttpRequestFactory(http11Client()))
                    .build()
                    .post()
                    .uri(executeUri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON);

            if (StringUtils.hasText(properties.getApiKey())) {
                spec.header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey());
            }

            return spec.body(request).retrieve().body(PistonExecuteResponse.class);
        } catch (RestClientResponseException ex) {
            String body = ex.getResponseBodyAsString();
            throw new PistonExecutionException("Piston request failed at " + executeUri
                    + " with java version " + properties.getJavaVersion()
                    + ": HTTP " + ex.getStatusCode().value() + " " + body, ex);
        } catch (RestClientException ex) {
            throw new PistonExecutionException("Piston request failed at " + executeUri
                    + " with java version " + properties.getJavaVersion()
                    + ": " + ex.getMessage(), ex);
        }
    }

    private HttpClient http11Client() {
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (!StringUtils.hasText(baseUrl)) {
            return "http://localhost:2000/api/v2";
        }
        String normalized = baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}

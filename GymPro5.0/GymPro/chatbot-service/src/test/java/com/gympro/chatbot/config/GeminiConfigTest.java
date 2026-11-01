package com.gympro.chatbot.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link GeminiConfig}.
 * Verifies that both RestTemplate beans are created with the expected request factories.
 */
class GeminiConfigTest {

    @Test
    @DisplayName("geminiRestTemplate bean is created with configured timeouts")
    void geminiRestTemplate_isCreated() {
        GeminiConfig config = new GeminiConfig();
        ReflectionTestUtils.setField(config, "connectTimeoutMs", 10000);
        ReflectionTestUtils.setField(config, "readTimeoutMs", 30000);

        RestTemplate restTemplate = config.geminiRestTemplate();

        assertThat(restTemplate).isNotNull();
        assertThat(restTemplate.getRequestFactory()).isNotNull();
    }

    @Test
    @DisplayName("restTemplate bean is created for internal service calls")
    void restTemplate_isCreated() {
        GeminiConfig config = new GeminiConfig();

        RestTemplate restTemplate = config.restTemplate();

        assertThat(restTemplate).isNotNull();
        assertThat(restTemplate.getRequestFactory()).isNotNull();
    }
}

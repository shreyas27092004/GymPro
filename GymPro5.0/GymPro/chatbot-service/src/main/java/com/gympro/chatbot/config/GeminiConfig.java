package com.gympro.chatbot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Configures the RestTemplate used to call the Google Gemini API.
 *
 * <p>geminiRestTemplate — pre-configured with:
 * <ul>
 *   <li>10-second connect timeout</li>
 *   <li>30-second read timeout (Gemini responses can take a few seconds)</li>
 * </ul>
 * NOTE: Gemini authenticates via a query parameter (?key=...) rather than an
 * Authorization header, so no interceptor is needed here.  The API key is
 * appended to the URL inside {@link com.gympro.chatbot.service.ChatbotService}.
 *
 * <p>restTemplate — plain instance for any internal service calls (tighter timeouts).
 */
@Configuration
public class GeminiConfig {

    @Value("${gemini.connect.timeout.ms:10000}")
    private int connectTimeoutMs;

    @Value("${gemini.read.timeout.ms:30000}")
    private int readTimeoutMs;

    /**
     * RestTemplate for Gemini API calls — tuned for AI latency.
     * Injected into ChatbotService via @Qualifier("geminiRestTemplate").
     */
    @Bean(name = "geminiRestTemplate")
    public RestTemplate geminiRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
        return new RestTemplate(factory);
    }

    /**
     * Plain RestTemplate for internal microservice calls (no AI token).
     */
    @Bean(name = "restTemplate")
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        return new RestTemplate(factory);
    }
}

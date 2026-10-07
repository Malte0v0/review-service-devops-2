package org.example.reviewservice.health;

import lombok.extern.flogger.Flogger;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Component("externalApi")
public class ExternalApiHealthIndicator implements HealthIndicator {
    private static final Logger log = LoggerFactory.getLogger(ExternalApiHealthIndicator.class);
    private final RestClient restClient;
    public final String url;

    public ExternalApiHealthIndicator(
            @Value("${external.api.url:https://www.githubstatus.com/api/v2/status.json}") String url) {

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(2));
        this.url = url;
        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .build();
    }

    @Override
    public Health health() {
        long startTime = System.currentTimeMillis();
        try {
            restClient.get().uri(url).retrieve().toBodilessEntity();


            long responseTime = System.currentTimeMillis() - startTime;
            return Health.up().up().withDetail("url", url).withDetail("responseTime", responseTime).build();
        } catch (Exception e) {
            log.warn("api health check failed: {}", e.getClass().getSimpleName());
            return Health.down()
                    .withDetail("url", url)
                    .withDetail("error", e.getClass().getSimpleName())
                    .build();
        }

    }

}

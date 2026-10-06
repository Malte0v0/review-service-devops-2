package org.example.reviewservice.health;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("externalApi")
public class ExternalApiHealthIndicator implements HealthIndicator {
    @Override
    public Health health() {
        return Health.up().build();
    }



}

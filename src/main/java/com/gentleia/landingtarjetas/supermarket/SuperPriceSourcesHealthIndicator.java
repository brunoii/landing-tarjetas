package com.gentleia.landingtarjetas.supermarket;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class SuperPriceSourcesHealthIndicator implements HealthIndicator {

    private final SuperPriceSourceRepository priceSourceRepository;

    public SuperPriceSourcesHealthIndicator(SuperPriceSourceRepository priceSourceRepository) {
        this.priceSourceRepository = priceSourceRepository;
    }

    @Override
    public Health health() {
        try {
            int activePriceSourceCount = priceSourceRepository.findByActiveTrueOrderByNameAsc().size();
            return Health.up()
                    .withDetail("activePriceSourceCount", activePriceSourceCount)
                    .withDetail("scope", "stage11-price-sources")
                    .build();
        } catch (RuntimeException exception) {
            return Health.down(exception)
                    .withDetail("scope", "stage11-price-sources")
                    .build();
        }
    }
}

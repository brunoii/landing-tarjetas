package com.gentleia.landingtarjetas;

import static org.assertj.core.api.Assertions.assertThat;

import com.gentleia.landingtarjetas.supermarket.SuperPriceSource;
import com.gentleia.landingtarjetas.supermarket.SuperPriceSourceRepository;
import com.gentleia.landingtarjetas.supermarket.SuperPriceSourcesHealthIndicator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SuperPriceSourcesHealthIndicatorTests {

    @Autowired
    private SuperPriceSourcesHealthIndicator healthIndicator;

    @Autowired
    private SuperPriceSourceRepository superPriceSourceRepository;

    @BeforeEach
    void cleanDatabase() {
        superPriceSourceRepository.deleteAll();
    }

    @Test
    void reportsStage11PriceSourceSignalWithActiveCount() {
        superPriceSourceRepository.save(new SuperPriceSource("Ticket proveedor"));

        var health = healthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails())
                .containsEntry("scope", "stage11-price-sources")
                .containsEntry("activePriceSourceCount", 1);
    }
}

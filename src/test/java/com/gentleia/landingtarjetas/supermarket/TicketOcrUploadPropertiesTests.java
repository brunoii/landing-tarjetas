package com.gentleia.landingtarjetas.supermarket;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class TicketOcrUploadPropertiesTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesConfiguration.class);

    @Test
    void bindsCliDiagnosticProperties() {
        contextRunner.withPropertyValues(
                "app.super.ticket-ocr-upload.executable=/usr/bin/tesseract",
                "app.super.ticket-ocr-upload.languages=spa+eng",
                "app.super.ticket-ocr-upload.datapath=/srv/tessdata",
                "app.super.ticket-ocr-upload.timeout=7s")
                .run(context -> {
                    TicketOcrUploadProperties properties = context.getBean(TicketOcrUploadProperties.class);
                    assertThat(properties.getExecutable()).isEqualTo("/usr/bin/tesseract");
                    assertThat(properties.getLanguages()).isEqualTo("spa+eng");
                    assertThat(properties.getDatapath()).isEqualTo("/srv/tessdata");
                    assertThat(properties.getTimeout()).hasSeconds(7);
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(TicketOcrUploadProperties.class)
    static class PropertiesConfiguration {
    }
}

package com.gentleia.landingtarjetas.supermarket;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.super.ticket-ocr-upload")
public class TicketOcrUploadProperties {

    private long maxFileSizeBytes = 1_048_576L;
    /** Maximum width or height accepted before decoding an OCR image. */
    private int maxDecodedDimension = 4_096;
    private String executable = "tesseract";
    private Duration timeout = Duration.ofSeconds(15);
    private String datapath = "";
    private String languages = "spa+eng";

    public long getMaxFileSizeBytes() {
        return maxFileSizeBytes;
    }

    public void setMaxFileSizeBytes(long maxFileSizeBytes) {
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    public int getMaxDecodedDimension() {
        return maxDecodedDimension;
    }

    public void setMaxDecodedDimension(int maxDecodedDimension) {
        this.maxDecodedDimension = maxDecodedDimension;
    }

    public String getExecutable() {
        return executable;
    }

    public void setExecutable(String executable) {
        this.executable = executable;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    public String getDatapath() {
        return datapath;
    }

    public void setDatapath(String datapath) {
        this.datapath = datapath;
    }

    public String getLanguages() {
        return languages;
    }

    public void setLanguages(String languages) {
        this.languages = languages;
    }
}

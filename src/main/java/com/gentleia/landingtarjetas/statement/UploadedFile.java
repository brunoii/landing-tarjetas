package com.gentleia.landingtarjetas.statement;

import java.time.Instant;

import com.gentleia.landingtarjetas.shared.CardBrand;
import com.gentleia.landingtarjetas.shared.ParsingStatus;
import com.gentleia.landingtarjetas.shared.Provider;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "uploaded_files")
public class UploadedFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String originalFilename;

    @Column(nullable = false)
    private String storagePath;

    @Column(length = 120)
    private String contentType;

    @Column(nullable = false)
    private long sizeBytes;

    @Column(length = 64)
    private String checksumSha256;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private Provider detectedProvider;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private CardBrand detectedCardBrand;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private ParsingStatus parsingStatus = ParsingStatus.NOT_STARTED;

    @Column(length = 500)
    private String parsingMessage;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected UploadedFile() {
    }

    public UploadedFile(String originalFilename, String storagePath, String contentType, long sizeBytes) {
        this.originalFilename = originalFilename;
        this.storagePath = storagePath;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public String getChecksumSha256() {
        return checksumSha256;
    }

    public void setChecksumSha256(String checksumSha256) {
        this.checksumSha256 = checksumSha256;
    }

    public Provider getDetectedProvider() {
        return detectedProvider;
    }

    public void setDetectedProvider(Provider detectedProvider) {
        this.detectedProvider = detectedProvider;
    }

    public CardBrand getDetectedCardBrand() {
        return detectedCardBrand;
    }

    public void setDetectedCardBrand(CardBrand detectedCardBrand) {
        this.detectedCardBrand = detectedCardBrand;
    }

    public ParsingStatus getParsingStatus() {
        return parsingStatus;
    }

    public void setParsingStatus(ParsingStatus parsingStatus) {
        this.parsingStatus = parsingStatus;
    }

    public String getParsingMessage() {
        return parsingMessage;
    }

    public void setParsingMessage(String parsingMessage) {
        this.parsingMessage = parsingMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

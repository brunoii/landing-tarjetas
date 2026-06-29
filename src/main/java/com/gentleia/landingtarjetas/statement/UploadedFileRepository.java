package com.gentleia.landingtarjetas.statement;

import java.util.List;

import com.gentleia.landingtarjetas.shared.ParsingStatus;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UploadedFileRepository extends JpaRepository<UploadedFile, Long> {

    List<UploadedFile> findByParsingStatusOrderByCreatedAtDesc(ParsingStatus parsingStatus);
}

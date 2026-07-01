package com.gentleia.landingtarjetas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.gentleia.landingtarjetas.projection.InstallmentProjectionRepository;
import com.gentleia.landingtarjetas.shared.ParsingStatus;
import com.gentleia.landingtarjetas.shared.StatementStatus;
import com.gentleia.landingtarjetas.statement.CardStatementRepository;
import com.gentleia.landingtarjetas.statement.UploadedFileRepository;
import com.gentleia.landingtarjetas.transaction.StatementTransactionRepository;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class StatementUploadControllerTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UploadedFileRepository uploadedFileRepository;
    @Autowired
    private CardStatementRepository statementRepository;
    @Autowired
    private StatementTransactionRepository transactionRepository;
    @Autowired
    private InstallmentProjectionRepository projectionRepository;

    @BeforeEach
    void cleanDatabase() {
        projectionRepository.deleteAll();
        transactionRepository.deleteAll();
        statementRepository.deleteAll();
        uploadedFileRepository.deleteAll();
    }

    @Test
    void uploadAcceptsFilesMultipartPdfAndReturnsDraftMetadata() throws Exception {
        MockMultipartFile file = new MockMultipartFile("files", "synthetic.pdf", "application/pdf", pdfWithText("""
                Synthetic Santander Visa statement
                Closing date: 2026-06-25
                Due date: 2026-07-10
                Total pesos: ARS 1234.56
                Total USD: USD 45.67
                Minimum payment: ARS 100.00
                TX: 2026-06-01 | Fictional bookstore | ARS 120.50 | installment 2/6
                """));

        mockMvc.perform(multipart("/api/statements/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.files[0].uploadedFile.originalFilename").value("synthetic.pdf"))
                .andExpect(jsonPath("$.files[0].uploadedFile.detectedProvider").doesNotExist())
                .andExpect(jsonPath("$.files[0].uploadedFile.detectedCardBrand").doesNotExist())
                .andExpect(jsonPath("$.files[0].detectedProvider").value("SANTANDER"))
                .andExpect(jsonPath("$.files[0].detectedCardBrand").value("VISA"))
                .andExpect(jsonPath("$.files[0].parsingStatus").value("PARSED"))
                .andExpect(jsonPath("$.files[0].parserName").value("SantanderVisaParser"))
                .andExpect(jsonPath("$.files[0].draftStatement.status").value("DRAFT"))
                .andExpect(jsonPath("$.files[0].draftStatement.provider").value("SANTANDER"))
                .andExpect(jsonPath("$.files[0].draftStatement.cardBrand").value("VISA"))
                .andExpect(jsonPath("$.files[0].draftStatement.uploadedFile.detectedProvider").doesNotExist())
                .andExpect(jsonPath("$.files[0].draftStatement.uploadedFile.detectedCardBrand").doesNotExist())
                .andExpect(jsonPath("$.files[0].draftStatement.transactions[0].description").value("Fictional bookstore"));

        assertThat(statementRepository.findAll()).singleElement()
                .satisfies(statement -> assertThat(statement.getStatus()).isEqualTo(StatementStatus.DRAFT));
    }

    @Test
    void uploadConvertsCorruptedPdfExtractionFailureIntoFailedMetadata() throws Exception {
        MockMultipartFile file = new MockMultipartFile("files", "corrupted.pdf", "application/pdf",
                "not a valid pdf".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/statements/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.files[0].uploadedFile.originalFilename").value("corrupted.pdf"))
                .andExpect(jsonPath("$.files[0].uploadedFile.parsingStatus").value("FAILED"))
                .andExpect(jsonPath("$.files[0].parsingStatus").value("FAILED"))
                .andExpect(jsonPath("$.files[0].error").value("PDF upload could not be processed. No statement text or raw PDF content was stored."))
                .andExpect(jsonPath("$.files[0].draftStatement").value(nullValue()));

        assertThat(statementRepository.findAll()).isEmpty();
        assertThat(uploadedFileRepository.findAll()).singleElement().satisfies(uploadedFile -> {
            assertThat(uploadedFile.getParsingStatus()).isEqualTo(ParsingStatus.FAILED);
            assertThat(uploadedFile.getChecksumSha256()).hasSize(64);
        });
    }

    @Test
    void uploadDoesNotAcceptUndocumentedSingularFileParameter() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "synthetic.pdf", "application/pdf", pdfWithText("""
                Synthetic Santander Visa statement
                Due date: 2026-07-10
                Total pesos: ARS 100.00
                TX: 2026-06-01 | Fictional purchase | ARS 10.00
                """));

        mockMvc.perform(multipart("/api/statements/upload").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("At least one PDF file is required"));

        assertThat(uploadedFileRepository.findAll()).isEmpty();
        assertThat(statementRepository.findAll()).isEmpty();
    }

    private static byte[] pdfWithText(String text) throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                content.setLeading(14);
                content.newLineAtOffset(50, 750);
                for (String line : text.strip().split("\\R")) {
                    content.showText(line.strip());
                    content.newLine();
                }
                content.endText();
            }
            document.save(output);
            return output.toByteArray();
        }
    }
}

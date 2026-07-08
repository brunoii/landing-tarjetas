package com.gentleia.landingtarjetas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Comparator;

import com.gentleia.landingtarjetas.projection.InstallmentProjectionRepository;
import com.gentleia.landingtarjetas.shared.ParsingStatus;
import com.gentleia.landingtarjetas.shared.Provider;
import com.gentleia.landingtarjetas.shared.StatementStatus;
import com.gentleia.landingtarjetas.statement.CardStatement;
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
                Fecha de cierre actual
                25/06/2026
                Fecha de vencimiento actual
                10/07
                Total a pagar en pesos
                $ 1.234,56
                Total USD
                USD 45,67
                Pago mínimo
                $ 100,00
                01/06 Fictional bookstore cuota 2/6 $ 120,50
                """));

        mockMvc.perform(multipart("/api/statements/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.files[0].uploadedFile.originalFilename").value("synthetic.pdf"))
                .andExpect(jsonPath("$.files[0].uploadedFile.detectedProvider").doesNotExist())
                .andExpect(jsonPath("$.files[0].uploadedFile.detectedCardBrand").doesNotExist())
                .andExpect(jsonPath("$.files[0].detectedProvider").value("SANTANDER"))
                .andExpect(jsonPath("$.files[0].detectedCardBrand").value("VISA"))
                .andExpect(jsonPath("$.files[0].parsingStatus").value("PARSED"))
                .andExpect(jsonPath("$.files[0].parserName").value("Santander Visa"))
                .andExpect(jsonPath("$.files[0].draftStatement.status").value("DRAFT"))
                .andExpect(jsonPath("$.files[0].draftStatement.provider").value("SANTANDER"))
                .andExpect(jsonPath("$.files[0].draftStatement.cardBrand").value("VISA"))
                .andExpect(jsonPath("$.files[0].draftStatement.uploadedFile.detectedProvider").doesNotExist())
                .andExpect(jsonPath("$.files[0].draftStatement.uploadedFile.detectedCardBrand").doesNotExist())
                .andExpect(jsonPath("$.files[0].draftStatement.transactions[0].description").value("Fictional bookstore cuota 2/6"))
                .andExpect(content().string(not(containsString("Synthetic Santander Visa statement"))))
                .andExpect(content().string(not(containsString("SantanderVisaParser"))));

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
                .andExpect(jsonPath("$.files[0].error").value("No se pudo procesar el PDF. No se almacenó texto del resumen ni contenido original del PDF."))
                .andExpect(jsonPath("$.files[0].draftStatement").value(nullValue()));

        assertThat(statementRepository.findAll()).isEmpty();
        assertThat(uploadedFileRepository.findAll()).singleElement().satisfies(uploadedFile -> {
            assertThat(uploadedFile.getParsingStatus()).isEqualTo(ParsingStatus.FAILED);
            assertThat(uploadedFile.getChecksumSha256()).hasSize(64);
        });
    }

    @Test
    void uploadNaranjaXPlanZDraftCanBeConfirmedWithInstallmentMetadata() throws Exception {
        MockMultipartFile file = new MockMultipartFile("files", "naranja-plan-z.pdf", "application/pdf", pdfWithText("""
                Synthetic Tarjeta Naranja account summary
                Fecha de cierre actual Fecha de vencimiento actual
                25/07/2026 10/08/2026
                Total a pagar $ 30.000,00
                07/07/2026 compra gomez pardo 30000 z
                """));

        mockMvc.perform(multipart("/api/statements/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.files[0].parsingStatus").value("PARSED"))
                .andExpect(jsonPath("$.files[0].draftStatement.status").value("DRAFT"))
                .andExpect(jsonPath("$.files[0].draftStatement.transactions[0].type").value("INSTALLMENT"))
                .andExpect(jsonPath("$.files[0].draftStatement.transactions[0].currentInstallment").value(1))
                .andExpect(jsonPath("$.files[0].draftStatement.transactions[0].totalInstallments").value(3));

        Long statementId = statementRepository.findAll().get(0).getId();

        mockMvc.perform(post("/api/statements/{id}/confirm", statementId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.transactions[0].type").value("INSTALLMENT"))
                .andExpect(jsonPath("$.transactions[0].currentInstallment").value(1))
                .andExpect(jsonPath("$.transactions[0].totalInstallments").value(3))
                .andExpect(jsonPath("$.transactions[0].notes").value("Plan Z detectado: se cargó como 1 de 3 cuotas."));

        assertThat(statementRepository.findById(statementId)).get()
                .satisfies(statement -> assertThat(statement.getStatus()).isEqualTo(StatementStatus.CONFIRMED));
    }

    @Test
    void reuploadConfirmedStatementSkipsDuplicateOperationTransactionsButAllowsDifferentStatementScope() throws Exception {
        MockMultipartFile first = new MockMultipartFile("files", "synthetic-idempotent.pdf", "application/pdf", pdfWithText("""
                Synthetic Santander Visa statement
                Fecha de cierre actual
                25/06/2026
                Fecha de vencimiento actual
                10/07/2026
                Total a pagar en pesos
                $ 100,00
                TX: 2026-06-01 | Fixture operation purchase | ARS 10.00 | op: SYN-001
                """));

        mockMvc.perform(multipart("/api/statements/upload").file(first))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.files[0].draftStatement.transactions[0].operationNumber").value("SYN-001"));
        Long firstStatementId = newestDraftStatementId();
        mockMvc.perform(post("/api/statements/{id}/confirm", firstStatementId))
                .andExpect(status().isOk());

        MockMultipartFile duplicate = new MockMultipartFile("files", "synthetic-idempotent.pdf", "application/pdf", first.getBytes());
        mockMvc.perform(multipart("/api/statements/upload").file(duplicate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.files[0].draftStatement.transactions.length()").value(0));
        Long duplicateStatementId = newestDraftStatementId();
        mockMvc.perform(post("/api/statements/{id}/confirm", duplicateStatementId))
                .andExpect(status().isOk());

        assertThat(transactionRepository.findByStatementPaymentMonthOrderByTransactionDateAsc(LocalDate.of(2026, 7, 1)))
                .hasSize(1);

        MockMultipartFile nextMonth = new MockMultipartFile("files", "synthetic-next-month.pdf", "application/pdf", pdfWithText("""
                Synthetic Santander Visa statement
                Fecha de cierre actual
                25/07/2026
                Fecha de vencimiento actual
                10/08/2026
                Total a pagar en pesos
                $ 100,00
                TX: 2026-07-01 | Fixture operation purchase | ARS 10.00 | op: SYN-001
                """));
        mockMvc.perform(multipart("/api/statements/upload").file(nextMonth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.files[0].draftStatement.transactions.length()").value(1));
        Long nextMonthStatementId = newestDraftStatementId();
        mockMvc.perform(post("/api/statements/{id}/confirm", nextMonthStatementId))
                .andExpect(status().isOk());

        assertThat(transactionRepository.findAll()).hasSize(2);
    }

    @Test
    void sameOperationNumberInSamePaymentMonthIsAllowedForDifferentProviderCardScope() throws Exception {
        MockMultipartFile santanderVisa = new MockMultipartFile("files", "synthetic-santander-scope.pdf", "application/pdf", pdfWithText("""
                Synthetic Santander Visa statement
                Fecha de cierre actual
                25/06/2026
                Fecha de vencimiento actual
                10/07/2026
                Total a pagar en pesos
                $ 100,00
                TX: 2026-06-01 | Fixture scoped purchase | ARS 10.00 | op: SYN-SCOPE
                """));

        mockMvc.perform(multipart("/api/statements/upload").file(santanderVisa))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.files[0].draftStatement.transactions.length()").value(1));
        Long santanderStatementId = newestDraftStatementId();
        mockMvc.perform(post("/api/statements/{id}/confirm", santanderStatementId))
                .andExpect(status().isOk());

        MockMultipartFile naranjaVisa = new MockMultipartFile("files", "synthetic-naranja-scope.pdf", "application/pdf", pdfWithText("""
                Synthetic Tarjeta Naranja Visa account summary
                Fecha de cierre actual
                25/06/2026
                Fecha de vencimiento actual
                10/07/2026
                Total a pagar $ 100,00
                TX: 2026-06-01 | Fixture scoped purchase | ARS 10.00 | op: SYN-SCOPE
                """));

        mockMvc.perform(multipart("/api/statements/upload").file(naranjaVisa))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.files[0].draftStatement.provider").value("NARANJA_X"))
                .andExpect(jsonPath("$.files[0].draftStatement.cardBrand").value("VISA"))
                .andExpect(jsonPath("$.files[0].draftStatement.transactions.length()").value(1))
                .andExpect(jsonPath("$.files[0].draftStatement.transactions[0].operationNumber").value("SYN-SCOPE"));
        Long naranjaStatementId = newestDraftStatementId();
        mockMvc.perform(post("/api/statements/{id}/confirm", naranjaStatementId))
                .andExpect(status().isOk());

        assertThat(transactionRepository.findByStatementPaymentMonthOrderByTransactionDateAsc(LocalDate.of(2026, 7, 1)))
                .hasSize(2);
        assertThat(statementRepository.findAll())
                .hasSize(2)
                .extracting(CardStatement::getProvider)
                .containsExactlyInAnyOrder(
                        Provider.SANTANDER,
                        Provider.NARANJA_X
                );
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
                .andExpect(jsonPath("$.error").value("Se requiere al menos un archivo PDF"));

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

    private Long newestDraftStatementId() {
        return statementRepository.findAll().stream()
                .filter(statement -> statement.getStatus() == StatementStatus.DRAFT)
                .max(Comparator.comparing(CardStatement::getId))
                .map(CardStatement::getId)
                .orElseThrow();
    }
}

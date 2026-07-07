package com.gentleia.landingtarjetas.statement;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.gentleia.landingtarjetas.shared.ParsingStatus;
import com.gentleia.landingtarjetas.shared.StatementStatus;
import com.gentleia.landingtarjetas.statement.parser.ParsedStatement;
import com.gentleia.landingtarjetas.statement.parser.ParsedTransaction;
import com.gentleia.landingtarjetas.statement.parser.StatementParser;
import com.gentleia.landingtarjetas.statement.parser.StatementParserRegistry;
import com.gentleia.landingtarjetas.transaction.StatementTransaction;
import com.gentleia.landingtarjetas.transaction.StatementTransactionRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class StatementUploadService {

    private static final String IN_MEMORY_STORAGE_MARKER = "IN_MEMORY_ONLY";

    private final PdfTextExtractionService pdfTextExtractionService;
    private final StatementParserRegistry parserRegistry;
    private final CardStatementRepository statementRepository;
    private final StatementTransactionRepository transactionRepository;
    private final UploadedFileRepository uploadedFileRepository;

    public StatementUploadService(PdfTextExtractionService pdfTextExtractionService,
                                   StatementParserRegistry parserRegistry,
                                   CardStatementRepository statementRepository,
                                   StatementTransactionRepository transactionRepository,
                                   UploadedFileRepository uploadedFileRepository) {
        this.pdfTextExtractionService = pdfTextExtractionService;
        this.parserRegistry = parserRegistry;
        this.statementRepository = statementRepository;
        this.transactionRepository = transactionRepository;
        this.uploadedFileRepository = uploadedFileRepository;
    }

    @Transactional
    public StatementUploadResponse upload(MultipartFile[] files) {
        List<MultipartFile> allFiles = files == null ? List.of() : Arrays.asList(files);
        if (allFiles.isEmpty()) {
            throw new IllegalArgumentException("Se requiere al menos un archivo PDF");
        }

        List<StatementUploadResultResponse> results = allFiles.stream()
                .map(this::processFile)
                .toList();
        return new StatementUploadResponse(results);
    }

    private StatementUploadResultResponse processFile(MultipartFile file) {
        UploadedFile uploadedFile = new UploadedFile(
                safeFilename(file),
                IN_MEMORY_STORAGE_MARKER,
                file == null ? null : file.getContentType(),
                file == null ? 0L : file.getSize()
        );

        try {
            pdfTextExtractionService.validate(file);
            byte[] pdfBytes = file.getBytes();
            uploadedFile.setChecksumSha256(pdfTextExtractionService.sha256(pdfBytes));
            String extractedText = pdfTextExtractionService.extractText(pdfBytes);

            Optional<StatementParser> parser = parserRegistry.detect(extractedText);
            if (parser.isEmpty()) {
                uploadedFile.setParsingStatus(ParsingStatus.PENDING);
                uploadedFile.setParsingMessage("No se detectó un formato de resumen compatible");
                uploadedFile = uploadedFileRepository.save(uploadedFile);
                return result(uploadedFile, null, List.of("No se detectó un formato de resumen compatible"), null, null);
            }

            ParsedStatement parsedStatement = parser.get().parse(extractedText);
            CardStatement draftStatement = createDraftStatement(parsedStatement, uploadedFile);
            uploadedFile = draftStatement.getUploadedFile();
            uploadedFile.setDetectedProvider(parsedStatement.provider());
            uploadedFile.setDetectedCardBrand(parsedStatement.cardBrand());
            uploadedFile.setParsingStatus(ParsingStatus.PARSED);
            uploadedFile.setParsingMessage(parsingMessage(parsedStatement.warnings()));
            CardStatement savedStatement = statementRepository.save(draftStatement);

            return result(savedStatement.getUploadedFile(), parser.get(), parsedStatement.warnings(), null,
                    StatementDetailResponse.from(savedStatement));
        } catch (IllegalArgumentException | IOException exception) {
            uploadedFile.setParsingStatus(ParsingStatus.FAILED);
            uploadedFile.setParsingMessage(safeMessage(exception));
            uploadedFile = uploadedFileRepository.save(uploadedFile);
            return result(uploadedFile, null, List.of(), safeMessage(exception), null);
        }
    }

    private CardStatement createDraftStatement(ParsedStatement parsedStatement, UploadedFile uploadedFile) {
        CardStatement statement = new CardStatement(parsedStatement.provider(), parsedStatement.cardBrand());
        statement.setStatus(StatementStatus.DRAFT);
        statement.setCardAlias(parsedStatement.cardAlias());
        statement.setClosingDate(parsedStatement.closingDate());
        statement.setDueDate(parsedStatement.dueDate());
        statement.setPaymentMonth(parsedStatement.paymentMonth());
        statement.setTotalPesos(parsedStatement.totalPesos());
        statement.setTotalUsd(parsedStatement.totalUsd());
        statement.setMinimumPaymentPesos(parsedStatement.minimumPaymentPesos());
        statement.setUploadedFile(uploadedFile);
        Set<String> existingKeys = confirmedTransactionKeys(statement);
        Set<String> currentKeys = new HashSet<>();
        for (ParsedTransaction parsedTransaction : parsedStatement.transactions()) {
            StatementTransaction transaction = new StatementTransaction(statement, parsedTransaction.description(), parsedTransaction.type());
            transaction.setTransactionDate(parsedTransaction.transactionDate());
            transaction.setAmountPesos(parsedTransaction.amountPesos());
            transaction.setAmountUsd(parsedTransaction.amountUsd());
            transaction.setCurrentInstallment(parsedTransaction.currentInstallment());
            transaction.setTotalInstallments(parsedTransaction.totalInstallments());
            transaction.setOperationNumber(parsedTransaction.operationNumber());
            transaction.setNotes(parsedTransaction.notes());
            String key = StatementTransactionIdentity.key(statement, transaction);
            if (existingKeys.contains(key) || currentKeys.contains(key)) {
                continue;
            }
            currentKeys.add(key);
            statement.addTransaction(transaction);
        }
        return statement;
    }

    private Set<String> confirmedTransactionKeys(CardStatement statement) {
        Set<String> keys = new HashSet<>();
        if (statement.getPaymentMonth() == null) {
            return keys;
        }
        for (StatementTransaction transaction : transactionRepository.findConfirmedWithFilters(
                statement.getPaymentMonth(), statement.getCardBrand(), null, null)) {
            if (StatementTransactionIdentity.sameStatementScope(statement, transaction.getStatement())) {
                keys.add(StatementTransactionIdentity.key(transaction.getStatement(), transaction));
            }
        }
        return keys;
    }

    private StatementUploadResultResponse result(UploadedFile uploadedFile, StatementParser parser, List<String> warnings,
                                                String error, StatementDetailResponse draftStatement) {
        return new StatementUploadResultResponse(
                UploadedFileResponse.from(uploadedFile),
                uploadedFile.getDetectedProvider(),
                uploadedFile.getDetectedCardBrand(),
                uploadedFile.getParsingStatus(),
                parser == null ? null : parser.name(),
                warnings,
                error,
                draftStatement
        );
    }

    private String safeFilename(MultipartFile file) {
        if (file == null || file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()) {
            return "unknown.pdf";
        }
        return file.getOriginalFilename();
    }

    private String parsingMessage(List<String> warnings) {
        if (warnings == null || warnings.isEmpty()) {
            return "Resumen en borrador creado a partir del formato detectado";
        }
        return "Resumen en borrador creado con advertencias: " + String.join("; ", warnings);
    }

    private String safeMessage(Exception exception) {
        if (exception instanceof IOException) {
            return "No se pudo procesar el PDF. No se almacenó texto del resumen ni contenido original del PDF.";
        }
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return "No se pudo procesar el PDF";
        }
        return message;
    }
}

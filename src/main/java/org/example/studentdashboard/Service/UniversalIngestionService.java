package org.example.studentdashboard.Service;

import com.github.pjfanning.xlsx.StreamingReader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.*;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

@Service
public class UniversalIngestionService {

    private final VectorStore documentVectorStore;
    private final ChatClient visionClient;
    private final JdbcTemplate jdbcTemplate;

    public UniversalIngestionService(@Qualifier("documentVectorStore") VectorStore documentVectorStore, AnthropicChatModel chatModel, JdbcTemplate jdbcTemplate) {
        this.documentVectorStore = documentVectorStore;
        this.visionClient = ChatClient.builder(chatModel).build();
        this.jdbcTemplate = jdbcTemplate;
    }

    public void verifyVectorCommit(String fileName) {
        System.out.println("Verifying database commit for: " + fileName);
        String sql = "SELECT COUNT(*) FROM document_store WHERE metadata->>'fileName' = ?";

        for (int i = 0; i < 15; i++) {
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, fileName);
            if (count != null && count > 0) {
                System.out.println("Commit confirmed in " + (i + 1) + " seconds.");
                return;
            }
            try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        throw new RuntimeException("TIMEOUT: File '" + fileName + "' not indexed within 15 seconds.");
    }

    private void deleteByFileName(String fileName) {
        try {
            String sql = "DELETE FROM document_store WHERE metadata->>'fileName' = ?";
            int deleted = jdbcTemplate.update(sql, fileName);
            System.out.println("Deleted " + deleted + " existing vectors for: " + fileName);
        } catch (Exception e) {
            System.err.println("Warning: Could not delete existing vectors for " + fileName + ": " + e.getMessage());
        }
    }

    private void safeAdd(List<Document> docs, String fileName) {
        try {
            deleteByFileName(fileName);
            documentVectorStore.add(docs);
        } catch (Exception e) {
            throw new RuntimeException("CRITICAL: Failed to write to VectorStore: " + e.getMessage(), e);
        }
    }

    public void ingestFile(MultipartFile file) throws Exception {
        String contentType = file.getContentType();
        String fileName = file.getOriginalFilename();

        System.out.println("contentType: "+contentType);

        if (fileName == null || fileName.endsWith(".crdownload")) {
            throw new RuntimeException("UPLOAD BLOCKED: File is incomplete (partial download).");
        }

        if (contentType == null) throw new RuntimeException("Illegal file format!");

        if (contentType.equals("application/pdf")) processPdfHybrid(file);
        else if (contentType.contains("spreadsheetml") || contentType.contains("excel")) processExcel(file);
        else if (contentType.startsWith("image/")) processRawImage(file.getBytes(), fileName, contentType);
        else if (contentType.equals("text/csv")) processCsv(file);
        else processGenericText(file);

        verifyVectorCommit(fileName);
    }

    // Hybrid approach: Fast text-first, intelligent Vision fallback
    private void processPdfHybrid(MultipartFile file) throws Exception {
        TokenTextSplitter splitter = new TokenTextSplitter();

        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFTextStripper stripper = new PDFTextStripper();
            PDFRenderer renderer = new PDFRenderer(document);

            for (int page = 0; page < document.getNumberOfPages(); page++) {
                stripper.setStartPage(page + 1);
                stripper.setEndPage(page + 1);
                String pageText = stripper.getText(document).trim();

                // Adaptive Decision: If text is sparse or highly symbolic, use Vision.
                if (pageText.length() < 100 || isGibberish(pageText)) {
                    System.out.println("Page " + (page + 1) + " is visual/sparse, switching to Vision.");
                    BufferedImage image = renderer.renderImageWithDPI(page, 200);
                    ByteArrayOutputStream bios = new ByteArrayOutputStream();
                    ImageIO.write(image, "png", bios);
                    processRawImage(bios.toByteArray(), file.getOriginalFilename() + "_page_" + (page + 1), "image/png");
                } else {
                    System.out.println("pageText: " + pageText);
                    Document textDoc = new Document("Source: " + file.getOriginalFilename() + "\nPage: " + (page + 1) + "\n\n" + pageText,
                            Map.of("fileName", file.getOriginalFilename(), "contentType", "document"));
                    safeAdd(splitter.apply(List.of(textDoc)), file.getOriginalFilename());
                }
            }
        }
    }

    private boolean isGibberish(String text) {
        long specialCharCount = text.chars().filter(ch -> !Character.isLetterOrDigit(ch) && !Character.isWhitespace(ch)).count();
        return (double) specialCharCount / text.length() > 0.3;
    }

    private void processRawImage(byte[] imageBytes, String fileName, String mimeType) {
        Media imageMedia = new Media(MimeTypeUtils.parseMimeType(mimeType), new ByteArrayResource(imageBytes));
        String aiGeneratedDescription = visionClient.prompt()
                .user(u -> u.text("Extract text and describe diagrams.").media(imageMedia))
                .call().content();

        Document doc = new Document("Source: " + fileName + "\n\n" + aiGeneratedDescription,
                Map.of("fileName", fileName, "contentType", "vision_extracted_page"));

        safeAdd(new TokenTextSplitter().apply(List.of(doc)), fileName);
    }

    private void processExcel(MultipartFile file) throws Exception {
        TokenTextSplitter splitter = new TokenTextSplitter();
        String fileName = file.getOriginalFilename();
        StringBuilder batch = new StringBuilder();
        int rowCount = 0;

        try (Workbook workbook = StreamingReader.builder().rowCacheSize(10).bufferSize(4096).open(file.getInputStream())) {
            for (Sheet sheet : workbook) {
                for (Row row : sheet) {
                    for (Cell cell : row) batch.append(extractCellValue(cell)).append(", ");
                    if (++rowCount >= 500) {
                        saveBatch(batch.toString(), fileName, splitter);
                        batch.setLength(0);
                        rowCount = 0;
                    }
                }
            }
            if (batch.length() > 0) saveBatch(batch.toString(), fileName, splitter);
        }
    }

    public void processCsv(MultipartFile file) throws Exception {
        TokenTextSplitter splitter = new TokenTextSplitter();
        String fileName = file.getOriginalFilename();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            StringBuilder batch = new StringBuilder();
            int count = 0;
            while ((line = reader.readLine()) != null) {
                batch.append(line).append("\n");
                if (++count >= 500) {
                    saveBatch(batch.toString(), fileName, splitter);
                    batch.setLength(0);
                    count = 0;
                }
            }
            if (batch.length() > 0) saveBatch(batch.toString(), fileName, splitter);
        }
    }

    private void saveBatch(String text, String fileName, TokenTextSplitter splitter) {
        Document doc = new Document("Source: " + fileName + "\n\n" + text,
                Map.of("fileName", fileName, "contentType", "spreadsheet"));
        safeAdd(splitter.apply(List.of(doc)), fileName);
    }

    public void processGenericText(MultipartFile file) throws Exception {
        TikaDocumentReader reader = new TikaDocumentReader(new InputStreamResource(file.getInputStream()));
        List<Document> docs = reader.get().stream().map(d -> {
            d.getMetadata().put("fileName", file.getOriginalFilename());
            d.getMetadata().put("contentType", "document");
            return d;
        }).toList();
        safeAdd(new TokenTextSplitter().apply(docs), file.getOriginalFilename());
    }

    private String extractCellValue(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> "\"" + cell.getStringCellValue() + "\"";
            case NUMERIC -> String.valueOf(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }
}
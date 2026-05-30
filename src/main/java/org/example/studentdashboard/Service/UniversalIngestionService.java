package org.example.studentdashboard.Service;

import com.github.pjfanning.xlsx.StreamingReader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
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

    private final VectorStore vectorStore;
    private final ChatClient visionClient;

    public UniversalIngestionService(VectorStore vectorStore, AnthropicChatModel chatModel){
        this.vectorStore = vectorStore;
        this.visionClient = ChatClient.builder(chatModel).build();
    }

    public void ingestFile(MultipartFile file) throws Exception{
        String contentType = file.getContentType();
        String fileName = file.getOriginalFilename();

        if(contentType == null) throw new RuntimeException("Illegal file format!");
        else if(contentType.equals("application/pdf")) processPdfWithVision(file);
        else if(contentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") ||
          contentType.equals("application/vnd.ms-excel"))
            processExcel(file);
        else if(contentType.startsWith("image/")) processRawImage(file.getBytes(),fileName);
        else if(contentType.equals("text/csv")) processCsv(file);
        else processGenericText(file);
    }

    private void processPdfWithVision(MultipartFile file) throws Exception{
         try(PDDocument document = PDDocument.load(file.getInputStream())){
             PDFRenderer renderer = new PDFRenderer(document);

             for(int page = 0;page < document.getNumberOfPages();page++){
                 BufferedImage image = renderer.renderImageWithDPI(page,300);
                 ByteArrayOutputStream bios = new ByteArrayOutputStream();
                 ImageIO.write(image,"png",bios);
                 byte[] pageBytes = bios.toByteArray();
                 String fileName = file.getOriginalFilename() + "_page_" + (page + 1);
                 processRawImage(pageBytes,fileName);
             }

         }
    }

    private void processRawImage(byte[] imageBytes,String fileName){
        Media imageMedia = new Media(MimeTypeUtils.IMAGE_PNG,new ByteArrayResource(imageBytes));
        String prompt = "You are an elite academic extraction AI. Analyze this image (which may be a document page, exam paper, or diagram). " +
                "1. Extract ALL text verbatim. " +
                "2. If you see diagrams, drawings, circuits, or chemical structures, describe them in extreme mathematical and structural detail (e.g., list masses, resistors, IUPAC names). " +
                "Format the output cleanly so it is highly searchable by a database.";

        String aiGeneratedDescription = visionClient.prompt().user(u -> u.text(prompt).media(imageMedia)).call().content();
        Document visionDoc = new Document(aiGeneratedDescription,
                   Map.of("fileName",fileName,
                           "contentType","vision_extracted_page"));

        TokenTextSplitter splitter = new TokenTextSplitter();
        vectorStore.add(splitter.apply(List.of(visionDoc)));
    }

    private void processExcel(MultipartFile file) throws Exception{
        TokenTextSplitter splitter = new TokenTextSplitter();
        StringBuilder batchContent = new StringBuilder();
        String fileName = file.getOriginalFilename();
        int batchNumber = 1;
        int batchLimit = 500;
        int rowCount = 0;

        try(Workbook workbook = StreamingReader.builder().rowCacheSize(10).bufferSize(4096)
                .open(file.getInputStream())){

            for(Sheet sheet:workbook){
                batchContent.append("===SHEET: ").append(sheet.getSheetName()).append(" ===\n");

                for(Row row:sheet){
                     StringBuilder rowContent = new StringBuilder();
                     for(Cell cell:row){
                         rowContent.append(extractCellValue(cell) + ", ");
                     }
                     if(rowContent.length() > 0) rowContent.setLength(rowContent.length() - 2);
                     batchContent.append(rowContent);
                     rowCount++;

                     if(rowCount >= batchLimit){
                         saveBatchToVectorStore(batchContent.toString(),fileName,batchNumber,splitter);
                         batchContent.setLength(0);
                         rowCount = 0;
                         batchNumber++;
                     }

                }
                batchContent.append("\n");

            }

            if(batchContent.length() > 0 && !batchContent.toString().trim().isEmpty())
                saveBatchToVectorStore(batchContent.toString(),fileName,batchNumber,splitter);
        }
    }

    public void processCsv(MultipartFile file) throws Exception{
        TokenTextSplitter splitter = new TokenTextSplitter();
        StringBuilder batchContent = new StringBuilder();
        String fileName = file.getOriginalFilename();
        int batchLimit = 500;
        int batchNumber = 1;
        int rowCount = 0;

        try(BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))){
            String line;
            while((line = reader.readLine())!= null){
                batchContent.append(line).append("\n");
                rowCount++;
                if(rowCount >= batchLimit){
                    saveBatchToVectorStore(batchContent.toString(),fileName,batchNumber,splitter);
                    batchContent.setLength(0);
                    rowCount = 0;
                    batchNumber++;
                }
            }
            if(batchContent.length() > 0 && !batchContent.toString().trim().isEmpty())
              saveBatchToVectorStore(batchContent.toString(),fileName,batchNumber,splitter);
        }

    }

    private void saveBatchToVectorStore(String csvText,String fileName,int batchNumber,TokenTextSplitter splitter){
        Document document = new Document(csvText,Map.of("fileName",fileName,
                                        "batchNumber",batchNumber,
                                         "contentType","spreadsheet"));

        vectorStore.add(splitter.apply(List.of(document)));
    }


    private String extractCellValue(Cell cell){
         if(cell == null) return "";

         switch (cell.getCellType()){
             case STRING : return "\"" + cell.getStringCellValue() + "\"";
             case NUMERIC:
                 if(DateUtil.isCellDateFormatted(cell))
                     return cell.getDateCellValue().toString();
                 else{
                     double value = cell.getNumericCellValue();
                     return value == Math.floor(value)?String.valueOf((long) value):String.valueOf(value);
                 }
             case BOOLEAN:return String.valueOf(cell.getBooleanCellValue());
             default:return "";
         }
    }

    public void processGenericText(MultipartFile file) throws Exception{
        InputStreamResource resource = new InputStreamResource(file.getInputStream()){
            @Override
            public String getFilename(){
                return file.getOriginalFilename();
            }

            @Override
            public long contentLength(){
                 return file.getSize();
            }
        };

        TikaDocumentReader documentReader = new TikaDocumentReader(resource);
        List<Document> docs = documentReader.get();

        for(Document doc:docs){
            doc.getMetadata().put("fileName",file.getOriginalFilename());
        }
        TokenTextSplitter splitter = new TokenTextSplitter();
        vectorStore.add(splitter.apply(docs));
    }

}

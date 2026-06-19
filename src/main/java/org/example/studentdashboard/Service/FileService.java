package org.example.studentdashboard.Service;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import org.springframework.core.ParameterizedTypeReference;
import java.nio.charset.StandardCharsets;
import jakarta.transaction.Transactional;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.example.studentdashboard.CSVModels.StudentData;
import org.example.studentdashboard.Enums.Role;
import org.example.studentdashboard.Models.*;
import org.example.studentdashboard.Repositories.*;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.*;
import java.time.Instant;
import java.util.*;

@Service
public class FileService {

    private ChatClient chatClient;

    private StudentRepository studentRepository;

    private ExamRepository examRepository;

    private StudentExamRepository studentExamRepository;

    private GridFsTemplate gridFsTemplate;

    private DownloadStatusRepository statusRepository;

    private DownloadTicketRepository ticketRepository;

    private JWTService jwtService;

    public FileService(AnthropicChatModel anthropicChatModel,
                       StudentRepository studentRepository,
                       ExamRepository examRepository,
                       StudentExamRepository studentExamRepository,
                       GridFsTemplate gridFsTemplate,DownloadStatusRepository statusRepository,
                       DownloadTicketRepository ticketRepository,JWTService jwtService) {
        this.chatClient = ChatClient.builder(anthropicChatModel).build();
        this.studentRepository = studentRepository;
        this.examRepository = examRepository;
        this.studentExamRepository = studentExamRepository;
        this.gridFsTemplate = gridFsTemplate;
        this.statusRepository = statusRepository;
        this.ticketRepository = ticketRepository;
        this.jwtService = jwtService;
    }

    private static final int CHUNK_SIZE = 100;

    private static final String SYSTEM_PROMPT = """
            You are a CSV parser. You will receive a header row and a chunk of CSV data rows.
            Return ONLY a valid JSON array of student objects — no explanation, no markdown, no code blocks.
            
            Use the header names to map to these JSON fields (match by meaning, not exact name):
            {
              "rollNo":                       header like "# ID NO",
              "name":                         header like "STUDENT NAME",
              "center":                       header like "CENTRE",
              "phone":                        header like "PHONE",
              "city":                         header like "CITY",
              "classNum":                     header like "CLASS",
              "mathsTotalQuestions":          header containing "MATHS" + "Total Questions",
              "mathsAttemptedQuestions":      header containing "MATHS" + "Attempted",
              "mathsCorrectAnswers":          header containing "MATHS" + "Correct",
              "mathsWrongAnswers":            header containing "MATHS" + "Wrong",
              "mathsPositiveMarks":           header containing "MATHS" + "Positive",
              "mathsNegativeMarks":           header containing "MATHS" + "Negative",
              "mathsMarksScored":             header containing "MATHS" + "Total Marks",
              "mathsTotalTimeSpent":          header containing "MATHS" + "Total Time",
              "mathsAvgTimeEachQuestion":     header containing "MATHS" + "Avg Time",
              "mathsRank":                    RANK column immediately after MATHS section,
              "physicsTotalQuestions":        header containing "PHYSICS" + "Total Questions",
              "physicsAttemptedQuestions":    header containing "PHYSICS" + "Attempted",
              "physicsCorrectAnswers":        header containing "PHYSICS" + "Correct",
              "physicsWrongAnswers":          header containing "PHYSICS" + "Wrong",
              "physicsPositiveMarks":         header containing "PHYSICS" + "Positive",
              "physicsNegativeMarks":         header containing "PHYSICS" + "Negative",
              "physicsMarksScored":           header containing "PHYSICS" + "Total Marks",
              "physicsTotalTimeSpent":        header containing "PHYSICS" + "Total Time",
              "physicsAvgTimeEachQuestion":   header containing "PHYSICS" + "Avg Time",
              "physicsRank":                  RANK column immediately after PHYSICS section,
              "chemistryTotalQuestions":      header containing "CHEMISTRY" + "Total Questions",
              "chemistryAttemptedQuestions":  header containing "CHEMISTRY" + "Attempted",
              "chemistryCorrectAnswers":      header containing "CHEMISTRY" + "Correct",
              "chemistryWrongAnswers":        header containing "CHEMISTRY" + "Wrong",
              "chemistryPositiveMarks":       header containing "CHEMISTRY" + "Positive",
              "chemistryNegativeMarks":       header containing "CHEMISTRY" + "Negative",
              "chemistryMarksScored":         header containing "CHEMISTRY" + "Total Marks",
              "chemistryTotalTimeSpent":      header containing "CHEMISTRY" + "Total Time",
              "chemistryAvgTimeEachQuestion": header containing "CHEMISTRY" + "Avg Time",
              "chemistryRank":                RANK column immediately after CHEMISTRY section,
              "totalQuestions":               header containing "TOTAL" + "Total Questions",
              "totalAttemptedQuestions":      header containing "TOTAL" + "Attempted",
              "totalCorrectAnswers":          header containing "TOTAL" + "Correct",
              "totalWrongAnswers":            header containing "TOTAL" + "Wrong",
              "totalPositiveMarks":           header containing "TOTAL" + "Positive",
              "totalNegativeMarks":           header containing "TOTAL" + "Negative",
              "totalMarks":                   header containing "TOTAL" + "Total Marks",
              "totalTimeSpent":               header containing "TOTAL" + "Total Time",
              "avgTimeEachQuestion":          header containing "TOTAL" + "Avg Time",
              "rank":                         RANK column immediately after TOTAL section,
              "questionsIncorrect":           header like "Qs Incorrect",
              "questionsNotAttempted":        header like "Qs Not Attempted",
              "timeOutside":   header like "TIME OUTSIDE",
              "examStartTime": header like "EXAM STARTED AT",
              "examEndTime":   header like "EXAM ENDED AT"
            }
            - For duplicate RANK headers, assign by which subject section they appear after
            - Only include rows where rollNo is purely numeric
            - For empty values use null
            - IGNORE all columns after "Qs Not Attempted" and "INACTIVE Student"
            - There will be hundreds of extra columns after — skip all of them completely
            """;


    public GridFSFile getFileLabel(String fileId){
        GridFSFile gridFSFile = gridFsTemplate.findOne(new Query(Criteria.where("_id").is(new ObjectId(fileId))));
        if(gridFSFile == null) throw new RuntimeException("File not found for the given id");
        return gridFSFile;
    }

    public String getMetaInfo(String attribute,GridFSFile file){
        Document metaData = file.getMetadata();
        if(metaData != null){
            if(metaData.containsKey(attribute)){
                return metaData.getString(attribute);
            }
        }

        return null;
    }

    public double getFileSizeInMb(GridFSFile file){
        double bytes = file.getLength();
        double mb = bytes/(1024 * 1024);
        return Math.round(mb*100.0)/100.0;

    }
    public List<FileResponse> getAllFileLabels(String token){
        String role = jwtService.getRole(token);
        String rollNumber = jwtService.getRollNumber(token);
        List<FileResponse> list = new ArrayList<>();
        Query query = new Query().with(Sort.by(Sort.Direction.DESC,"uploadDate"));

        if(!"ADMIN".equalsIgnoreCase(role))
            query.addCriteria(Criteria.where("metadata.rollNumber").is(rollNumber));

        gridFsTemplate.find(query).forEach(file -> {
            double mb = getFileSizeInMb(file);

            String examTypeStr = getMetaInfo("examType",file);
            String examIdentifierStr = getMetaInfo("examIdentifier",file);
            String fileRollNumber = getMetaInfo("rollNumber", file);
            list.add(FileResponse.builder().id(file.getObjectId().toHexString())
                    .fileName(file.getFilename())
                    .size(mb)
                    .uploadDate(file.getUploadDate().toString())
                    .examType(examTypeStr)
                    .examIdentifier(examIdentifierStr)
                    .rollNumber(fileRollNumber)
                    .build()
            );
        });

        return list;
    }


    public void deleteFile(String fileId) {
        ObjectId objId = new ObjectId(fileId);
        gridFsTemplate.delete(new Query(Criteria.where("_id").is(objId)));
        Query chunkQuery = new Query(Criteria.where("files_id").is(objId));
        gridFsTemplate.delete(chunkQuery);
    }
    public FileResponse uploadFile(MultipartFile file, String examType, String rollNumber) throws IOException {
        String originalFileName = file.getOriginalFilename();
        String examIdentifier = getExamIdentifier(originalFileName);

        // 1. Delete the old file if it exists
        GridFSFile existingFile = gridFsTemplate.findOne(
                new Query(Criteria.where("filename").is(originalFileName)));
        if(existingFile != null) {
            deleteFile(existingFile.getObjectId().toString());
        }

        // 2. Save the new file
        OmrFile metaData = OmrFile.builder()
                .fileName(originalFileName)
                .fileType(file.getContentType())
                .fileSize(file.getSize())
                .uploadedAt(Instant.now())
                .examType(examType)
                .examIdentifier(examIdentifier)
                .rollNumber(rollNumber)
                .build();

        ObjectId fileId = gridFsTemplate.store(file.getInputStream(),
                file.getOriginalFilename(), file.getContentType(), metaData);
        GridFSFile newlySavedFile = gridFsTemplate.findOne(new Query(Criteria.where("_id").is(fileId)));

        return FileResponse.builder()
                .id(fileId.toHexString())
                .fileName(originalFileName)
                .size(getFileSizeInMb(newlySavedFile))
                .uploadDate(newlySavedFile.getUploadDate().toString())
                .examType(examType)
                .examIdentifier(examIdentifier)
                .rollNumber(rollNumber)
                .build();
    }



    public DownloadStatus getDownloadStatus(String fileId){
         return statusRepository.findById(fileId)
                 .orElse(new DownloadStatus(fileId,"NOT STARTED"));
    }

    public String generateTicket(String fileId){
        String ticketId = UUID.randomUUID().toString();
        Ticket ticket = new Ticket();
        ticket.setTicketId(ticketId);
        ticket.setEntityId(fileId);
        ticketRepository.save(ticket);

        return ticketId;
    }

    public ResponseEntity<StreamingResponseBody> downloadFile(String fileId,String ticketId) throws IOException{
        Ticket validTicket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Invalid or expired download ticket!"));

        if (!validTicket.getEntityId().equals(fileId)) {
            throw new RuntimeException("Ticket does not match the requested file!");
        }
        ticketRepository.deleteById(ticketId);
        GridFsResource gridFsResource = gridFsTemplate.getResource(getFileLabel(fileId));
        statusRepository.save(new DownloadStatus(fileId,"PENDING"));
        StreamingResponseBody responseBody = outputStream -> {
            try(InputStream inputStream = gridFsResource.getInputStream()){
                byte[] buffer = new byte[8192];
                int bytesRead;
                while((bytesRead = inputStream.read(buffer)) != -1){
                    outputStream.write(buffer,0,bytesRead);
                }
                outputStream.flush();
                statusRepository.save(new DownloadStatus(fileId,"COMPLETED"));
            } catch (IOException e) {
                statusRepository.save(new DownloadStatus(fileId,"FAILED"));
                throw new RuntimeException("Download Failed!");
            }
        };
        Long contentLength = gridFsResource.contentLength();
        String contentType = gridFsResource.getContentType();
        String disposition = String.format("attachment; filename=\"%s\"",gridFsResource.getFilename());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .contentLength(contentLength)
                .header(HttpHeaders.CONTENT_DISPOSITION,disposition)
                .body(responseBody);

    }


    public String getExamIdentifier(String fileName){

            return fileName.replace(".csv", "")
                    .replaceAll("\\s?\\(\\d+\\)", "").trim();

    }


    private char detectSeparator(String headerLine) {
        int commaCount = (int) headerLine.chars().filter(c -> c == ',').count();
        int semicolonCount = (int) headerLine.chars().filter(c -> c == ';').count();
        return semicolonCount > commaCount ? ';' : ',';
    }

    private List<StudentData> parseChunkWithClaude(String csvChunk, char separator) {
        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return chatClient.prompt()
                        .system(SYSTEM_PROMPT)
                        .user("The CSV separator is '" + separator + "'. Parse this CSV chunk:\n\n" + csvChunk)
                        .call()
                        .entity(new ParameterizedTypeReference<List<StudentData>>() {});
            } catch (Exception e) {
                System.err.printf("Attempt %d failed: %s%n", attempt, e.getMessage());
                if (attempt == maxRetries) throw new RuntimeException("Claude parsing failed after retries!");
                try { Thread.sleep(1000L * attempt); } catch (InterruptedException ignored) {}
            }
        }
        return Collections.emptyList();
    }

    private Integer extractTotalMarks(String headerLine, String subject) {
        char sep = detectSeparator(headerLine);
        return Arrays.stream(headerLine.split(String.valueOf(sep)))
                .filter(h -> h.contains(subject) && h.contains("Total Marks"))
                .flatMap(h -> Arrays.stream(h.trim().split("\\s+")))
                .filter(w -> w.matches("[0-9]+"))
                .findFirst()
                .map(Integer::parseInt)
                .orElse(null);
    }

    private Integer extractTotalStudents(String headerLine) {
        char sep = detectSeparator(headerLine);
        return Arrays.stream(headerLine.split(String.valueOf(sep)))
                .filter(h -> h.contains("RANK"))
                .flatMap(h -> Arrays.stream(h.trim().split("\\s+")))
                .filter(w -> w.matches("[0-9]+"))
                .findFirst()
                .map(Integer::parseInt)
                .orElse(null);
    }

    private void saveBatch(List<StudentData> batch, Long examId, Map<String, Long> studentMap) {
        // 1. Build and save students
        List<Student> students = new ArrayList<>();
        for (StudentData data : batch) {
            Student student = new Student();
            if (studentMap.containsKey(data.getRollNo())) {
                student.setId(studentMap.get(data.getRollNo()));
            }
            student.setRollNo(data.getRollNo());
            student.setName(data.getName());
            student.setCity(data.getCity());
            student.setPhone(data.getPhone());
            student.setClassNum(data.getClassNum());
            student.setRole(Role.STUDENT);
            students.add(student);
        }
        List<Student> savedStudents = studentRepository.saveAll(students);

        // 2. Update studentMap with any newly saved students
        savedStudents.forEach(s -> studentMap.putIfAbsent(s.getRollNo(), s.getId()));

        // 3. Build and save StudentExam records
        List<StudentExam> studentExams = new ArrayList<>();
        for (int i = 0; i < batch.size(); i++) {
            StudentData data = batch.get(i);
            studentExams.add(StudentExam.builder()
                    .exam(Exam.builder().id(examId).build())
                    .student(savedStudents.get(i))
                    // Physics
                    .physicsAttemptedQuestions(data.getPhysicsAttemptedQuestions())
                    .physicsCorrectAnswers(data.getPhysicsCorrectAnswers())
                    .physicsWrongAnswers(data.getPhysicsWrongAnswers())
                    .physicsPositiveMarks(data.getPhysicsPositiveMarks())
                    .physicsNegativeMarks(data.getPhysicsNegativeMarks())
                    .physicsMarksScored(data.getPhysicsMarksScored())
                    .physicsTotalTimeSpent(data.getPhysicsTotalTimeSpent())
                    .physicsAvgTimeEachQuestion(data.getPhysicsAvgTimeEachQuestion())
                    .physicsRank(data.getPhysicsRank())
                    // Maths
                    .mathsAttemptedQuestions(data.getMathsAttemptedQuestions())
                    .mathsCorrectAnswers(data.getMathsCorrectAnswers())
                    .mathsWrongAnswers(data.getMathsWrongAnswers())
                    .mathsPositiveMarks(data.getMathsPositiveMarks())
                    .mathsNegativeMarks(data.getMathsNegativeMarks())
                    .mathsMarksScored(data.getMathsMarksScored())
                    .mathsTotalTimeSpent(data.getMathsTotalTimeSpent())
                    .mathsAvgTimeEachQuestion(data.getMathsAvgTimeEachQuestion())
                    .mathsRank(data.getMathsRank())
                    // Chemistry
                    .chemistryAttemptedQuestions(data.getChemistryAttemptedQuestions())
                    .chemistryCorrectAnswers(data.getChemistryCorrectAnswers())
                    .chemistryWrongAnswers(data.getChemistryWrongAnswers())
                    .chemistryPositiveMarks(data.getChemistryPositiveMarks())
                    .chemistryNegativeMarks(data.getChemistryNegativeMarks())
                    .chemistryMarksScored(data.getChemistryMarksScored())
                    .chemistryTotalTimeSpent(data.getChemistryTotalTimeSpent())
                    .chemistryAvgTimeEachQuestion(data.getChemistryAvgTimeEachQuestion())
                    .chemistryRank(data.getChemistryRank())
                    // Overall
                    .totalAttemptedQuestions(data.getTotalAttemptedQuestions())
                    .totalCorrectAnswers(data.getTotalCorrectAnswers())
                    .totalWrongAnswers(data.getTotalWrongAnswers())
                    .totalPositiveMarks(data.getTotalPositiveMarks())
                    .totalNegativeMarks(data.getTotalNegativeMarks())
                    .totalMarks(data.getTotalMarks())
                    .totalTimeSpent(data.getTotalTimeSpent())
                    .avgTimeEachQuestion(data.getAvgTimeEachQuestion())
                    .rank(data.getRank())
                    .timeOutside(data.getTimeOutside())
                    .examStartTime(data.getExamStartTime())
                    .examEndTime(data.getExamEndTime())
                    .build());
        }
        studentExamRepository.saveAll(studentExams);
    }

    @Transactional
    public void bulkPushFileData(MultipartFile file, String examType) throws IOException {

        // 1. Build existing student map (rollNo → id)
        Map<String, Long> studentMap = new HashMap<>();
        studentRepository.findAll().forEach(student -> {
            if (!studentMap.containsKey(student.getRollNo()))
                studentMap.put(student.getRollNo(), student.getId());
        });

        // 2. Check duplicate exam
        String examIdentifier = getExamIdentifier(file.getOriginalFilename());
        if (examRepository.findByExamIdentifier(examIdentifier).isPresent())
            throw new RuntimeException("Records already present for this exam!");

        // 3. Read header and detect separator
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
        String headerLine = reader.readLine();
        char separator = detectSeparator(headerLine);

        Integer physicsMarks   = extractTotalMarks(headerLine, "PHYSICS");
        Integer mathsMarks     = extractTotalMarks(headerLine, "MATHS");
        Integer chemistryMarks = extractTotalMarks(headerLine, "CHEMISTRY");
        Integer totalStudentsFromHeader = extractTotalStudents(headerLine);
        // 5. Save Exam entity
        Exam exam = Exam.builder()
                .examType(examType)
                .examIdentifier(examIdentifier)
                .physicsTotalMarks(physicsMarks)
                .mathsTotalMarks(mathsMarks)
                .chemistryTotalMarks(chemistryMarks)
                .examTotalMarks(physicsMarks +
                                mathsMarks +
                                chemistryMarks
                )
                .totalStudentsAttempted(totalStudentsFromHeader)
                .build();
        Long examId = examRepository.save(exam).getId();

        // 6. Seed batch with first parsed student, continue reading rest
        List<String> csvChunk = new ArrayList<>();
        List<StudentData> studentBatch = new ArrayList<>();

        String line;
        while ((line = reader.readLine()) != null) {
            csvChunk.add(line);

            if (csvChunk.size() == CHUNK_SIZE) {
                List<StudentData> parsed = parseChunkWithClaude(
                        headerLine + "\n" + String.join("\n", csvChunk), separator);
                studentBatch.addAll(parsed);
                csvChunk.clear();
                saveBatch(studentBatch, examId, studentMap);
                studentBatch.clear();
            }
        }
        reader.close();

        // 7. Handle remaining CSV lines
        if (!csvChunk.isEmpty()) {
            List<StudentData> parsed = parseChunkWithClaude(
                    headerLine + "\n" + String.join("\n", csvChunk), separator);
            studentBatch.addAll(parsed);
        }

        // 8. Save remaining students
        if (!studentBatch.isEmpty()) {
            saveBatch(studentBatch, examId, studentMap);
        }

        // 9. Update total students on exam
        exam.setId(examId);
        examRepository.save(exam);
    }


}
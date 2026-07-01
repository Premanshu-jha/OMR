package org.example.studentdashboard.Service;

import com.mongodb.client.gridfs.model.GridFSFile;
import java.nio.charset.StandardCharsets;
import jakarta.transaction.Transactional;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.example.studentdashboard.CSVModels.StudentData;
import org.example.studentdashboard.Enums.Role;
import org.example.studentdashboard.Models.*;
import org.example.studentdashboard.Repositories.*;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.*;
import java.time.Instant;
import java.util.*;

@Service
public class FileService {

    private StudentRepository studentRepository;
    private ExamRepository examRepository;
    private StudentExamRepository studentExamRepository;
    private GridFsTemplate gridFsTemplate;
    private DownloadStatusRepository statusRepository;
    private DownloadTicketRepository ticketRepository;
    private JWTService jwtService;

    // Built once per file upload from the header line
    private Map<String, Integer> headerIndex = null;

    public FileService(StudentRepository studentRepository,
                       ExamRepository examRepository,
                       StudentExamRepository studentExamRepository,
                       GridFsTemplate gridFsTemplate,
                       DownloadStatusRepository statusRepository,
                       DownloadTicketRepository ticketRepository,
                       JWTService jwtService) {
        this.studentRepository = studentRepository;
        this.examRepository = examRepository;
        this.studentExamRepository = studentExamRepository;
        this.gridFsTemplate = gridFsTemplate;
        this.statusRepository = statusRepository;
        this.ticketRepository = ticketRepository;
        this.jwtService = jwtService;
    }

    private static final int CHUNK_SIZE = 100;

    // ─── Header index builder ────────────────────────────────────────────────

    private void buildHeaderIndex(String headerLine, char separator) {
        headerIndex = new HashMap<>();
        String[] headers = headerLine.split(String.valueOf(separator), -1);

        String currentSubject = null;
        boolean physicsRankSeen = false, mathsRankSeen = false,
                chemistryRankSeen = false, totalRankSeen = false;

        for (int i = 0; i < headers.length; i++) {
            String raw = headers[i].trim();
            String h = raw.toUpperCase();

            // Track which subject section we're in (for RANK disambiguation)
            if (h.contains("PHYSICS"))              currentSubject = "PHYSICS";
            else if (h.contains("MATHS") || h.contains("MATH")) currentSubject = "MATHS";
            else if (h.contains("CHEMISTRY"))       currentSubject = "CHEMISTRY";
            else if (h.contains("TOTAL"))           currentSubject = "TOTAL";

            // ── Identity / misc fields ──
            if (h.contains("ID") && h.contains("NO"))          { headerIndex.put("rollNo", i); continue; }
            if (h.contains("STUDENT") && h.contains("NAME"))   { headerIndex.put("name", i); continue; }
            if (h.contains("CENTRE") || h.contains("CENTER"))  { headerIndex.put("center", i); continue; }
            if (h.equals("PHONE"))                              { headerIndex.put("phone", i); continue; }
            if (h.equals("CITY"))                               { headerIndex.put("city", i); continue; }
            if (h.equals("CLASS"))                              { headerIndex.put("classNum", i); continue; }
            if (h.contains("TIME OUTSIDE"))                     { headerIndex.put("timeOutside", i); continue; }
            if (h.contains("EXAM STARTED"))                     { headerIndex.put("examStartTime", i); continue; }
            if (h.contains("EXAM ENDED"))                       { headerIndex.put("examEndTime", i); continue; }
            if (h.contains("QS INCORRECT") || (h.contains("INCORRECT") && !h.contains("EXAM"))) {
                headerIndex.put("questionsIncorrect", i); continue;
            }
            if (h.contains("NOT ATTEMPTED"))                    { headerIndex.put("questionsNotAttempted", i); continue; }

            // ── RANK (disambiguate by subject context) ──
            if (h.contains("RANK")) {
                if ("PHYSICS".equals(currentSubject) && !physicsRankSeen)          { headerIndex.put("physicsRank", i); physicsRankSeen = true; }
                else if ("MATHS".equals(currentSubject) && !mathsRankSeen)         { headerIndex.put("mathsRank", i); mathsRankSeen = true; }
                else if ("CHEMISTRY".equals(currentSubject) && !chemistryRankSeen) { headerIndex.put("chemistryRank", i); chemistryRankSeen = true; }
                else if (!totalRankSeen)                                            { headerIndex.put("rank", i); totalRankSeen = true; }
                continue;
            }

            // ── Subject fields ──
            for (String subj : new String[]{"PHYSICS", "MATHS", "CHEMISTRY", "TOTAL"}) {
                if (!h.contains(subj)) continue;
                String prefix = switch (subj) {
                    case "PHYSICS"   -> "physics";
                    case "MATHS"     -> "maths";
                    case "CHEMISTRY" -> "chemistry";
                    default          -> "total";
                };

                if      (h.contains("TOTAL QUESTIONS"))  { headerIndex.put(prefix + "TotalQuestions", i); }
                else if (h.contains("ATTEMPTED"))         { headerIndex.put(prefix + "AttemptedQuestions", i); }
                else if (h.contains("CORRECT"))           { headerIndex.put(prefix + "CorrectAnswers", i); }
                else if (h.contains("WRONG"))             { headerIndex.put(prefix + "WrongAnswers", i); }
                else if (h.contains("POSITIVE"))          { headerIndex.put(prefix + "PositiveMarks", i); }
                else if (h.contains("NEGATIVE"))          { headerIndex.put(prefix + "NegativeMarks", i); }
                else if (h.contains("MARKS SCORED"))      { headerIndex.put(prefix + "MarksScored", i); }
                else if (h.contains("TOTAL MARKS"))       { headerIndex.put(prefix + "MarksScored", i); }
                else if (h.contains("TOTAL TIME"))        { headerIndex.put(prefix + "TotalTimeSpent", i); }
                else if (h.contains("AVG TIME"))          { headerIndex.put(prefix + "AvgTimeEachQuestion", i); }
                // The lone "TOTAL" column (marks scored for overall)
                else if (subj.equals("TOTAL") && h.equals("TOTAL")) { headerIndex.put("totalMarks", i); }
                break;
            }
        }

        System.out.println("=== HEADER INDEX BUILT (" + headerIndex.size() + " mappings) ===");
        headerIndex.forEach((k, v) -> System.out.println("  " + k + " → col[" + v + "] = \"" + headers[v].trim() + "\""));
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private boolean matches(String header, String... keywords) {
        for (String kw : keywords) if (!header.contains(kw)) return false;
        return true;
    }

    private String get(String[] cols, String field) {
        Integer idx = headerIndex.get(field);
        if (idx == null || idx >= cols.length) return null;
        String val = cols[idx].trim();
        return val.isEmpty() ? null : val;
    }

    private Integer getInt(String[] cols, String field) {
        String val = get(cols, field);
        if (val == null) return null;
        try { return Integer.parseInt(val); }
        catch (NumberFormatException e) { return null; }
    }

    private Long getLong(String[] cols, String field) {
        String val = get(cols, field);
        if (val == null) return null;
        try { return Long.parseLong(val); }
        catch (NumberFormatException e) { return null; }
    }

    // ─── Manual CSV chunk parser (replaces parseChunkWithClaude) ─────────────

    private List<StudentData> parseChunk(String csvChunk, char separator, int chunkNumber) {
        System.out.println("\n=== PARSING CHUNK #" + chunkNumber + " ===");
        String[] lines = csvChunk.split("\n");
        // lines[0] is the header row (re-sent each call) — skip it, index already built
        List<StudentData> result = new ArrayList<>();
        int skipped = 0;

        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) { skipped++; continue; }

            String[] cols = line.split(String.valueOf(separator), -1);

            String rollNo = get(cols, "rollNo");
            if (rollNo == null || !rollNo.matches("[0-9]+")) {
                System.out.println("  [SKIP] line " + i + " — rollNo='" + rollNo + "' (not purely numeric)");
                skipped++;
                continue;
            }

            try {
                StudentData d = new StudentData();
                d.setRollNo(rollNo);
                d.setName(get(cols, "name"));
                d.setCenter(get(cols, "center"));
                d.setPhone(get(cols, "phone"));
                d.setCity(get(cols, "city"));
                d.setClassNum(getInt(cols, "classNum"));

                // Physics
                d.setPhysicsAttemptedQuestions(getInt(cols, "physicsAttemptedQuestions"));
                d.setPhysicsCorrectAnswers(getInt(cols, "physicsCorrectAnswers"));
                d.setPhysicsWrongAnswers(getInt(cols, "physicsWrongAnswers"));
                d.setPhysicsPositiveMarks(getInt(cols, "physicsPositiveMarks"));
                d.setPhysicsNegativeMarks(getInt(cols, "physicsNegativeMarks"));
                d.setPhysicsMarksScored(getInt(cols, "physicsMarksScored"));
                d.setPhysicsTotalTimeSpent(get(cols, "physicsTotalTimeSpent"));
                d.setPhysicsAvgTimeEachQuestion(get(cols, "physicsAvgTimeEachQuestion"));
                d.setPhysicsRank(getLong(cols, "physicsRank"));

                // Maths
                d.setMathsAttemptedQuestions(getInt(cols, "mathsAttemptedQuestions"));
                d.setMathsCorrectAnswers(getInt(cols, "mathsCorrectAnswers"));
                d.setMathsWrongAnswers(getInt(cols, "mathsWrongAnswers"));
                d.setMathsPositiveMarks(getInt(cols, "mathsPositiveMarks"));
                d.setMathsNegativeMarks(getInt(cols, "mathsNegativeMarks"));
                d.setMathsMarksScored(getInt(cols, "mathsMarksScored"));
                d.setMathsTotalTimeSpent(get(cols, "mathsTotalTimeSpent"));
                d.setMathsAvgTimeEachQuestion(get(cols, "mathsAvgTimeEachQuestion"));
                d.setMathsRank(getLong(cols, "mathsRank"));

                // Chemistry
                d.setChemistryAttemptedQuestions(getInt(cols, "chemistryAttemptedQuestions"));
                d.setChemistryCorrectAnswers(getInt(cols, "chemistryCorrectAnswers"));
                d.setChemistryWrongAnswers(getInt(cols, "chemistryWrongAnswers"));
                d.setChemistryPositiveMarks(getInt(cols, "chemistryPositiveMarks"));
                d.setChemistryNegativeMarks(getInt(cols, "chemistryNegativeMarks"));
                d.setChemistryMarksScored(getInt(cols, "chemistryMarksScored"));
                d.setChemistryTotalTimeSpent(get(cols, "chemistryTotalTimeSpent"));
                d.setChemistryAvgTimeEachQuestion(get(cols, "chemistryAvgTimeEachQuestion"));
                d.setChemistryRank(getLong(cols, "chemistryRank"));

                // Totals
                d.setTotalAttemptedQuestions(getInt(cols, "totalAttemptedQuestions"));
                d.setTotalCorrectAnswers(getInt(cols, "totalCorrectAnswers"));
                d.setTotalWrongAnswers(getInt(cols, "totalWrongAnswers"));
                d.setTotalPositiveMarks(getInt(cols, "totalPositiveMarks"));
                d.setTotalNegativeMarks(getInt(cols, "totalNegativeMarks"));
                d.setTotalMarks(getInt(cols, "totalMarksScored"));
                d.setTotalTimeSpent(get(cols, "totalTotalTimeSpent"));
                d.setAvgTimeEachQuestion(get(cols, "totalAvgTimeEachQuestion"));
                d.setRank(getLong(cols, "rank"));

                d.setTimeOutside(get(cols, "timeOutside"));
                d.setExamStartTime(get(cols, "examStartTime"));
                d.setExamEndTime(get(cols, "examEndTime"));

                result.add(d);
                System.out.println("  [OK] rollNo=" + d.getRollNo()
                        + " | name=" + d.getName()
                        + " | physicsMarks=" + d.getPhysicsMarksScored()
                        + " | mathsMarks=" + d.getMathsMarksScored()
                        + " | chemMarks=" + d.getChemistryMarksScored()
                        + " | total=" + d.getTotalMarks()
                        + " | rank=" + d.getRank());

            } catch (Exception e) {
                System.err.println("  [ERROR] line " + i + " rollNo=" + rollNo + " — " + e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println("=== CHUNK #" + chunkNumber + " RESULT: parsed=" + result.size() + ", skipped=" + skipped + " ===\n");
        return result;
    }

    // ─── Unchanged methods ────────────────────────────────────────────────────

    public GridFSFile getFileLabel(String fileId) {
        GridFSFile gridFSFile = gridFsTemplate.findOne(new Query(Criteria.where("_id").is(new ObjectId(fileId))));
        if (gridFSFile == null) throw new RuntimeException("File not found for the given id");
        return gridFSFile;
    }

    public String getMetaInfo(String attribute, GridFSFile file) {
        Document metaData = file.getMetadata();
        if (metaData != null && metaData.containsKey(attribute)) {
            return metaData.getString(attribute);
        }
        return null;
    }

    public double getFileSizeInMb(GridFSFile file) {
        double bytes = file.getLength();
        double mb = bytes / (1024 * 1024);
        return Math.round(mb * 100.0) / 100.0;
    }

    public List<FileResponse> getAllFileLabels(String token) {
        String role = jwtService.getRole(token);
        String rollNumber = jwtService.getRollNumber(token);
        List<FileResponse> list = new ArrayList<>();
        Query query = new Query().with(Sort.by(Sort.Direction.DESC, "uploadDate"));

        if (!"ADMIN".equalsIgnoreCase(role))
            query.addCriteria(Criteria.where("metadata.rollNumber").is(rollNumber));

        gridFsTemplate.find(query).forEach(file -> {
            double mb = getFileSizeInMb(file);
            String examTypeStr = getMetaInfo("examType", file);
            String examIdentifierStr = getMetaInfo("examIdentifier", file);
            String fileRollNumber = getMetaInfo("rollNumber", file);
            list.add(FileResponse.builder()
                    .id(file.getObjectId().toHexString())
                    .fileName(file.getFilename())
                    .size(mb)
                    .uploadDate(file.getUploadDate().toString())
                    .examType(examTypeStr)
                    .examIdentifier(examIdentifierStr)
                    .rollNumber(fileRollNumber)
                    .build());
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

        GridFSFile existingFile = gridFsTemplate.findOne(
                new Query(Criteria.where("filename").is(originalFileName)));
        if (existingFile != null) {
            deleteFile(existingFile.getObjectId().toString());
        }

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

    public DownloadStatus getDownloadStatus(String fileId) {
        return statusRepository.findById(fileId)
                .orElse(new DownloadStatus(fileId, "NOT STARTED"));
    }

    public String generateTicket(String fileId) {
        String ticketId = UUID.randomUUID().toString();
        Ticket ticket = new Ticket();
        ticket.setTicketId(ticketId);
        ticket.setEntityId(fileId);
        ticketRepository.save(ticket);
        return ticketId;
    }

    public ResponseEntity<StreamingResponseBody> downloadFile(String fileId, String ticketId) throws IOException {
        Ticket validTicket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Invalid or expired download ticket!"));
        if (!validTicket.getEntityId().equals(fileId)) {
            throw new RuntimeException("Ticket does not match the requested file!");
        }
        ticketRepository.deleteById(ticketId);
        GridFsResource gridFsResource = gridFsTemplate.getResource(getFileLabel(fileId));
        statusRepository.save(new DownloadStatus(fileId, "PENDING"));

        StreamingResponseBody responseBody = outputStream -> {
            try (InputStream inputStream = gridFsResource.getInputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
                statusRepository.save(new DownloadStatus(fileId, "COMPLETED"));
            } catch (IOException e) {
                statusRepository.save(new DownloadStatus(fileId, "FAILED"));
                throw new RuntimeException("Download Failed!");
            }
        };

        Long contentLength = gridFsResource.contentLength();
        String contentType = gridFsResource.getContentType();
        String disposition = String.format("attachment; filename=\"%s\"", gridFsResource.getFilename());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .contentLength(contentLength)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .body(responseBody);
    }

    public String getExamIdentifier(String fileName) {
        return fileName.replace(".csv", "")
                .replaceAll("\\s?\\(\\d+\\)", "").trim();
    }

    private char detectSeparator(String headerLine) {
        int commaCount = (int) headerLine.chars().filter(c -> c == ',').count();
        int semicolonCount = (int) headerLine.chars().filter(c -> c == ';').count();
        return semicolonCount > commaCount ? ';' : ',';
    }

    private Integer extractTotalMarks(String headerLine, String subject) {
        char sep = detectSeparator(headerLine);
        return Arrays.stream(headerLine.split(String.valueOf(sep)))
                .filter(h -> h.toUpperCase().contains(subject) && h.toUpperCase().contains("TOTAL MARKS"))
                .flatMap(h -> Arrays.stream(h.trim().split("\\s+")))
                .filter(w -> w.matches("[0-9]+"))
                .findFirst()
                .map(Integer::parseInt)
                .orElse(null);
    }

    private Integer extractTotalStudents(String headerLine) {
        char sep = detectSeparator(headerLine);
        return Arrays.stream(headerLine.split(String.valueOf(sep)))
                .filter(h -> h.toUpperCase().contains("RANK"))
                .flatMap(h -> Arrays.stream(h.trim().split("\\s+")))
                .filter(w -> w.matches("[0-9]+"))
                .findFirst()
                .map(Integer::parseInt)
                .orElse(null);
    }

    private void saveBatch(List<StudentData> batch, Long examId, Map<String, Long> studentMap) {
        System.out.println("\n--- saveBatch called: " + batch.size() + " students ---");

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
        savedStudents.forEach(s -> studentMap.putIfAbsent(s.getRollNo(), s.getId()));
        System.out.println("  Saved " + savedStudents.size() + " student records.");

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
                    .totalMarksScored(data.getTotalMarks())
                    .totalTimeSpent(data.getTotalTimeSpent())
                    .avgTimeEachQuestion(data.getAvgTimeEachQuestion())
                    .rank(data.getRank())
                    .timeOutside(data.getTimeOutside())
                    .examStartTime(data.getExamStartTime())
                    .examEndTime(data.getExamEndTime())
                    .build());
        }
        studentExamRepository.saveAll(studentExams);
        System.out.println("  Saved " + studentExams.size() + " studentExam records.");
    }

    @Transactional
    public void bulkPushFileData(MultipartFile file) throws IOException {
        System.out.println("\n========================================");
        System.out.println("=== bulkPushFileData START ===");
        System.out.println("========================================");

        Map<String, Long> studentMap = new HashMap<>();
        studentRepository.findAll().forEach(student -> {
            if (!studentMap.containsKey(student.getRollNo()))
                studentMap.put(student.getRollNo(), student.getId());
        });
        System.out.println("Existing students in DB: " + studentMap.size());

        // 2. Check duplicate exam
        String examIdentifier = getExamIdentifier(file.getOriginalFilename());

        String examType = null;
        if(examIdentifier.contains("MPT")) examType = "JEE-MAINS";
        else if(examIdentifier.contains("APT")) examType = "JEE-ADVANCED";
        else if(examIdentifier.contains("EPT")) examType = "EAPCET";
        else{
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Please check the file name! must have one of these present to identify exam type:[MPT,APT,EPT]"
            );

        }

        System.out.println("examIdentifier: " + examIdentifier);
        if (examRepository.findByExamIdentifier(examIdentifier).isPresent())
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Records already present for this exam!"
            );

        // 3. Read header and detect separator
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
        String headerLine = reader.readLine();
        char separator = detectSeparator(headerLine);
        System.out.println("Detected separator: '" + separator + "'");

        // 4. Build header index once
        buildHeaderIndex(headerLine, separator);

        // 5. Extract marks and student count from header
        Integer physicsMarks   = extractTotalMarks(headerLine, "PHYSICS");
        Integer mathsMarks     = extractTotalMarks(headerLine, "MATHS");
        Integer chemistryMarks = extractTotalMarks(headerLine, "CHEMISTRY");
        Integer totalStudentsFromHeader = extractTotalStudents(headerLine);
        System.out.println("physicsMarks=" + physicsMarks + " | mathsMarks=" + mathsMarks
                + " | chemistryMarks=" + chemistryMarks + " | totalStudents=" + totalStudentsFromHeader);

        // 6. Save Exam entity
        Exam exam = Exam.builder()
                .examType(examType)
                .examIdentifier(examIdentifier)
                .physicsTotalMarks(physicsMarks)
                .mathsTotalMarks(mathsMarks)
                .chemistryTotalMarks(chemistryMarks)
                .examTotalMarks(physicsMarks + mathsMarks + chemistryMarks)
                .totalStudentsAttempted(totalStudentsFromHeader)
                .build();
        Long examId = examRepository.save(exam).getId();
        System.out.println("Saved Exam with id=" + examId);

        // 7. Read CSV in chunks and parse
        List<String> csvChunk = new ArrayList<>();
        List<StudentData> studentBatch = new ArrayList<>();
        int chunkNumber = 0;
        int totalParsed = 0;

        String line;
        while ((line = reader.readLine()) != null) {
            csvChunk.add(line);

            if (csvChunk.size() == CHUNK_SIZE) {
                chunkNumber++;
                System.out.println("\n>>> Processing chunk #" + chunkNumber + " (" + csvChunk.size() + " lines)");
                List<StudentData> parsed = parseChunk(
                        headerLine + "\n" + String.join("\n", csvChunk), separator, chunkNumber);
                totalParsed += parsed.size();
                studentBatch.addAll(parsed);
                csvChunk.clear();
                saveBatch(studentBatch, examId, studentMap);
                studentBatch.clear();
            }
        }
        reader.close();

        // 8. Handle remaining lines
        if (!csvChunk.isEmpty()) {
            chunkNumber++;
            System.out.println("\n>>> Processing final chunk #" + chunkNumber + " (" + csvChunk.size() + " lines)");
            List<StudentData> parsed = parseChunk(
                    headerLine + "\n" + String.join("\n", csvChunk), separator, chunkNumber);
            totalParsed += parsed.size();
            studentBatch.addAll(parsed);
        }

        // 9. Save remaining students
        if (!studentBatch.isEmpty()) {
            saveBatch(studentBatch, examId, studentMap);
        }

        // 10. Update exam
        exam.setId(examId);
        examRepository.save(exam);

        System.out.println("\n========================================");
        System.out.println("=== bulkPushFileData COMPLETE ===");
        System.out.println("  Total chunks: " + chunkNumber);
        System.out.println("  Total students parsed: " + totalParsed);
        System.out.println("========================================\n");
    }
}
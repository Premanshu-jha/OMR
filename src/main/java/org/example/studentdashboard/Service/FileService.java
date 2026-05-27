package org.example.studentdashboard.Service;
import com.mongodb.client.gridfs.GridFSFindIterable;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.example.studentdashboard.CSVModels.ExamResults;
import org.example.studentdashboard.CSVModels.StudentData;
import org.example.studentdashboard.Models.*;
import org.example.studentdashboard.Repositories.DownloadStatusRepository;
import org.example.studentdashboard.Repositories.ExamRepository;
import org.example.studentdashboard.Repositories.StudentExamRepository;
import org.example.studentdashboard.Repositories.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.*;
import java.time.Instant;
import java.util.*;

@Service
public class FileService {
    @Autowired
    GridFsTemplate gridFsTemplate;

    @Autowired
    DownloadStatusRepository statusRepository;

    @Autowired
    StudentRepository studentRepository;

    @Autowired
    ExamRepository examRepository;

    @Autowired
    StudentExamRepository studentExamRepository;

    @PersistenceContext
    EntityManager entityManager;


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
    public List<FileResponse> getAllFileLabels(){
        List<FileResponse> list = new ArrayList<>();
        Query query = new Query().with(Sort.by(Sort.Direction.DESC,"uploadDate"));
        gridFsTemplate.find(query).forEach(file -> {
            double mb = getFileSizeInMb(file);

            String examTypeStr = getMetaInfo("examType",file);
            String examIdentifierStr = getMetaInfo("examIdentifier",file);
            list.add(FileResponse.builder().id(file.getObjectId().toHexString())
                    .fileName(file.getFilename())
                    .size(mb)
                    .uploadDate(file.getUploadDate().toString())
                    .examType(examTypeStr)
                    .examIdentifier(examIdentifierStr)
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
    public FileResponse uploadOmrFile(MultipartFile file, String examType) throws IOException {
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
                .build();
    }



    public DownloadStatus getDownloadStatus(String fileId){
         return statusRepository.findById(fileId)
                 .orElse(new DownloadStatus(fileId,"NOT STARTED"));
    }

    public ResponseEntity<StreamingResponseBody> downloadFile(@PathVariable String fileId) throws IOException{
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

    public List<StudentData> processCsvFile(InputStream inputStream){

        try(Reader reader = new BufferedReader(new InputStreamReader(inputStream))){

            CsvToBean<StudentData> csvToBean = new CsvToBeanBuilder<StudentData>(reader)
                    .withType(StudentData.class)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build();

            List<StudentData> list = new ArrayList<>();
            for(StudentData studentData:csvToBean){
                String id = studentData.getRollNo();
                if(id.matches("[0-9]+")) list.add(studentData);
                else break;
            }
           return list;
        }
         catch (Exception e){
             throw new RuntimeException("Error in processing csv file!");
         }
    }

    public GridFSFile getExamResultFile(String examType,String examIdentifier){
        Query query = new Query().with(Sort.by(Sort.Direction.DESC,"uploadDate"));
        GridFSFindIterable iterable = gridFsTemplate.find(query);
        for(GridFSFile file:iterable) {
            if (file.getFilename().contains("exam_results") && examType.equals(getMetaInfo("examType",file)) &&
                    examIdentifier.equals(getMetaInfo("examIdentifier",file))) {
                return file;
            }
        }
        throw new RuntimeException("No file with exam_results name found!");
    }

    public String getExamIdentifier(String fileName){
        if(fileName.contains("exam_results_")) {
            return fileName.replace("exam_results_", "").replace(".csv", "")
                    .replaceAll("\\s?\\(\\d+\\)", "").trim();
        }
        return null;
    }


    public ExamResults getExamResults(String examType,String examIdentifier) throws IOException {
                GridFSFile examFile = getExamResultFile(examType,examIdentifier);
                GridFsResource gridFsResource = gridFsTemplate.getResource(examFile);
                List<StudentData> list = processCsvFile(gridFsResource.getInputStream());
                if(list.size() > 0) {
                    ExamResults examResults = new ExamResults();
                    examResults.setStudentData(list);
                    examResults.setExamIdentifier(getExamIdentifier(examFile.getFilename()));

                    Integer physicsTotalMarks = list.get(0).getPhysicsTotalMarks();
                    Integer physicsTotalQuestions = list.get(0).getPhysicsTotalQuestions();

                    Integer mathsTotalMarks = list.get(0).getMathsTotalMarks();
                    Integer mathsTotalQuestions = list.get(0).getMathsTotalQuestions();

                    Integer chemistryTotalMarks = list.get(0).getChemistryTotalMarks();
                    Integer chemistryTotalQuestions = list.get(0).getChemistryTotalQuestions();

                    examResults.setPhysicsTotalMarks(physicsTotalMarks);
                    examResults.setMathsTotalMarks(mathsTotalMarks);
                    examResults.setChemistryTotalMarks(chemistryTotalMarks);
                    examResults.setExamTotalMarks(physicsTotalMarks + mathsTotalMarks + chemistryTotalMarks);

                    examResults.setPhysicsTotalQuestions(physicsTotalQuestions);
                    examResults.setMathsTotalQuestions(mathsTotalQuestions);
                    examResults.setChemistryTotalQuestions(chemistryTotalQuestions);
                    examResults.setExamTotalQuestions(physicsTotalQuestions + mathsTotalQuestions + chemistryTotalQuestions);

                    examResults.setTotalStudentsAttempted(list.size());
                    return examResults;
                }
                else throw new RuntimeException("No students available!");
        }

    @Transactional
    public void bulkPushFileData(String examType,String examIdentifier) throws IOException {
        Map<String,Long> studentMap = new HashMap<>();
        studentRepository.findAll().forEach(student -> {
            String rollNo = student.getRollNo();
            if(!studentMap.containsKey(rollNo))
                 studentMap.put(rollNo,student.getId());
        });

        ExamResults examResults = getExamResults(examType,examIdentifier);
        Optional<Exam> optionalExam = examRepository.findByExamIdentifier(examResults.getExamIdentifier());
        if(optionalExam.isPresent()) throw new RuntimeException("Records allready present for this exam!");
        else{
            Exam exam = Exam.builder()
                    .examType(examType)
                    .examIdentifier(examResults.getExamIdentifier())
                    .examTotalMarks(examResults.getExamTotalMarks())
                    .physicsTotalMarks(examResults.getPhysicsTotalMarks())
                    .mathsTotalMarks(examResults.getMathsTotalMarks())
                    .chemistryTotalMarks(examResults.getChemistryTotalMarks())
                    .totalStudentsAttempted(examResults.getTotalStudentsAttempted())
                    .build();
            Long examId = examRepository.save(exam).getId();
            List<StudentData> studentRecords = examResults.getStudentData();
            for (int i = 0; i < studentRecords.size(); i++) {
                StudentData data = studentRecords.get(i);
                String rollNo = data.getRollNo();

                Student student = new Student();
                if (studentMap.containsKey(rollNo)) {
                    student.setId(studentMap.get(rollNo));
                }
                student.setRollNo(rollNo);
                student.setCity(data.getCity());
                student.setName(data.getName());
                student.setPhone(data.getPhone());
                student.setClassNum(data.getClassNum());
                Student savedStudent = studentRepository.save(student);


                StudentExam studentExam = StudentExam.builder()
                        .exam(Exam.builder().id(examId).build())
                        .student(savedStudent)
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
                        .build();

                studentExamRepository.save(studentExam);

                // 3. The Batch Flush/Clear logic
                if (i > 0 && i % 100 == 0) {
                    entityManager.flush();
                    entityManager.clear();
                }
            }
            entityManager.flush();
        }

    }


}
package org.example.studentdashboard.Tools;

import org.example.studentdashboard.Models.Exam;
import org.example.studentdashboard.Models.Student;
import org.example.studentdashboard.Models.StudentExam;
import org.example.studentdashboard.Repositories.ExamRepository;
import org.example.studentdashboard.Repositories.StudentExamRepository;
import org.example.studentdashboard.Repositories.StudentRepository;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class StudentDashBoardTools {

    private final StudentRepository studentRepository;
    private final StudentExamRepository studentExamRepository;
    private final ExamRepository examRepository;
    private final VectorStore documentVectorStore;

    public StudentDashBoardTools(StudentRepository studentRepository,
                                 StudentExamRepository studentExamRepository,
                                 ExamRepository examRepository,
                                 @Qualifier("documentVectorStore") VectorStore documentVectorStore,
                                 AnthropicChatModel chatModel){
        this.studentRepository = studentRepository;
        this.studentExamRepository = studentExamRepository;
        this.examRepository = examRepository;
        this.documentVectorStore = documentVectorStore;
    }

    @Tool(description = "Call this to retrieve personal details like name, phone, city, or class for a specific student using their roll number.")
    public Optional<Student> getStudentProfile(String rollNo){
        return studentRepository.findByRollNo(rollNo);
    }

    @Tool(description = "Call this to retrieve detailed subject marks, exam scores, and time metrics for a student when provided a roll number.")
    public List<StudentExam> getStudentPerformanceMetrics(String rollNo){
        return studentExamRepository.findByStudent_RollNo(rollNo);
    }

    @Tool(description = "Call this to resolve a student's name into their unique roll number. Useful when the user provides a name instead of an ID.")
    public List<Student> findStudentsByName(String name){
        return studentRepository.findByNameContainingIgnoreCase(name);
    }

    @Tool(description = "Call this to get global exam statistics, total possible marks, or metadata when given an exam identifier.")
    public Optional<Exam> getExamMetaData(String examIdentifier){
        return examRepository.findByExamIdentifier(examIdentifier);
    }

    @Tool(description = "Call this to compare a student's performance against the entire class for a specific exam identifier.")
    public List<StudentExam> getAllPerformanceForExam(String examIdentifier) {
        return studentExamRepository.findByExam_ExamIdentifier(examIdentifier);
    }

    @Tool(description = "Call this to answer questions about rankings or top students. Specify the subject ('physics', 'maths', 'chemistry') or leave null for total aggregate marks.")
    public String getTopPerformersBySubject(String subject, int limit) {
        if (limit > 100) limit = 100;
        Pageable page = PageRequest.of(0, limit);
        List<StudentExam> results;
        String subjectKey = (subject == null) ? "total" : subject.toLowerCase();

        switch (subjectKey) {
            case "physics" -> results = studentExamRepository.findTopByPhysicsMarksDesc(page);
            case "maths" -> results = studentExamRepository.findTopByMathsMarksDesc(page);
            case "chemistry" -> results = studentExamRepository.findTopByChemistryMarksDesc(page);
            case "total" -> results = studentExamRepository.findTopMarksDesc(page);
            default -> {
                return "Subject not recognized. Choose 'physics', 'maths', 'chemistry', or omit for 'total'.";
            }
        }

        if (results.isEmpty()) return "No records found.";

        StringBuilder sb = new StringBuilder("Top " + limit + " performers in " + subjectKey + ":\n");
        for (StudentExam se : results) {
            int score = switch (subjectKey) {
                case "physics" -> se.getPhysicsMarksScored();
                case "maths" -> se.getMathsMarksScored();
                case "chemistry" -> se.getChemistryMarksScored();
                case "total" -> se.getTotalMarksScored();
                default -> 0;
            };
            sb.append("- ").append(se.getStudent().getName())
                    .append(" (Exam: ").append(se.getExam().getExamIdentifier())
                    .append(") Score: ").append(score).append("\n");
        }
        return sb.toString();
    }

    @Tool(name = "searchDocumentDatabase", description = "Call this for ANY question about uploaded files, exam papers, instructions, diagrams, or textual content from PDFs/Excel files. Use this if the user asks about the content of a file.")
    public String searchDocumentDatabase(String query) {
        SearchRequest searchRequest = SearchRequest.builder()
                .query(query)
                .topK(10)
                .filterExpression(new FilterExpressionBuilder()
                        .in("contentType",
                                "vision_extracted_page",
                                "spreadsheet",
                                "document",
                                "application/pdf"
                        )
                        .build())
                .build();

        List<Document> docs = documentVectorStore.similaritySearch(searchRequest);

        if (docs.isEmpty()) {
            return "No documents found in the database. Please verify the file was uploaded correctly.";
        }

        StringBuilder context = new StringBuilder("I found the following file information in the database:\n");
        for (Document doc : docs) {
            String fileName = (String) doc.getMetadata().getOrDefault("fileName", "Unknown File");
            context.append("File: ").append(fileName).append("\nContent: ").append(doc.getText()).append("\n---\n");
        }

        return context.toString();
    }
}
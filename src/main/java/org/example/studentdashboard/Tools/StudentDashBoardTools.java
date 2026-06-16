package org.example.studentdashboard.Tools;

import org.example.studentdashboard.Models.Exam;
import org.example.studentdashboard.Models.Student;
import org.example.studentdashboard.Models.StudentExam;
import org.example.studentdashboard.Repositories.ExamRepository;
import org.example.studentdashboard.Repositories.StudentExamRepository;
import org.example.studentdashboard.Repositories.StudentRepository;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document; // Added missing import
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.vectorstore.SearchRequest; // Added missing import
import org.springframework.ai.vectorstore.VectorStore; // Added missing import
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
    private final VectorStore documentVectorStore; // Added dependency


    public StudentDashBoardTools(StudentRepository studentRepository,
                                 StudentExamRepository studentExamRepository,
                                 ExamRepository examRepository,
                                 @Qualifier("documentVectorStore") VectorStore documentVectorStore, // Added dependency
                                 AnthropicChatModel chatModel){
        this.studentRepository = studentRepository;
        this.studentExamRepository = studentExamRepository;
        this.examRepository = examRepository;
        this.documentVectorStore = documentVectorStore; // Added dependency
    }

    @Tool(description = "Get basic student profile data (name, phone, city, class number) by roll number.")
    public Optional<Student> getStudentProfile(String rollNo){
        return studentRepository.findByRollNo(rollNo);
    }

    @Tool(description = "Get detailed exam performance, subject marks, and time spent for a student, by roll number.")
    public List<StudentExam> getStudentPerformanceMetrics(String rollNo){
        return studentExamRepository.findByStudent_RollNo(rollNo);
    }

    @Tool(description = "Find students by name and return matching roll numbers.")
    public List<Student> findStudentsByName(String name){
        return studentRepository.findByNameContainingIgnoreCase(name);
    }

    @Tool(description = "Get total possible marks and global stats for a specific exam by its identifier.")
    public Optional<Exam> getExamMetaData(String examIdentifier){
        return examRepository.findByExamIdentifier(examIdentifier);
    }

    @Tool(description = "Get a list of all StudentExams for a specific exam identifier. Useful for comparison.")
    public List<StudentExam> getAllPerformanceForExam(String examIdentifier) {
        return studentExamRepository.findByExam_ExamIdentifier(examIdentifier);
    }

    @Tool(description = "Get a summary of top N performers for a subject (physics, maths, chemistry) or total marks across all exams.")
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
                case "total" -> se.getTotalMarks();
                default -> 0;
            };
            sb.append("- ").append(se.getStudent().getName())
                    .append(" (Exam: ").append(se.getExam().getExamIdentifier())
                    .append(") Score: ").append(score).append("\n");
        }
        return sb.toString();
    }

    @Tool(name = "searchDocumentDatabase", description = "Searches the document database for specific files.")
    public String searchDocumentDatabase(String query) {
        SearchRequest searchRequest = SearchRequest.builder()
                .query(query)
                .topK(5)
                .filterExpression(new FilterExpressionBuilder()
                        .in("contentType",
                                "vision_extracted_page",
                                "spreadsheet",
                                "document"
                        )
                        .build())
                .build();

        List<Document> docs = documentVectorStore.similaritySearch(searchRequest);
        System.out.println("docs: "+docs);
        if (docs.isEmpty()) {
            return "No documents found in the database for: " + query;
        }

        StringBuilder context = new StringBuilder("I found the following file information in the database:\n");
        for (Document doc : docs) {
            String fileName = (String) doc.getMetadata().getOrDefault("fileName", "Unknown File");
            context.append("File: ").append(fileName).append("\nContent: ").append(doc.getText()).append("\n---\n");
        }

        return context.toString();
    }
}
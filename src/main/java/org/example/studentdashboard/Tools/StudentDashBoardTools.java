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
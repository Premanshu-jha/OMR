package org.example.studentdashboard.Tools;

import org.example.studentdashboard.Models.Exam;
import org.example.studentdashboard.Models.Student;
import org.example.studentdashboard.Models.StudentExam;
import org.example.studentdashboard.Repositories.ExamRepository;
import org.example.studentdashboard.Repositories.StudentExamRepository;
import org.example.studentdashboard.Repositories.StudentRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class StudentDashBoardTools {

    private final StudentRepository studentRepository;
    private final StudentExamRepository studentExamRepository;
    private final ExamRepository examRepository;

    private final ChatClient claudeClient;

    public StudentDashBoardTools(StudentRepository studentRepository,
                                 StudentExamRepository studentExamRepository,
                                 ExamRepository examRepository,
                                 @Qualifier("anthropicChatClient") ChatClient claudeClient){
        this.studentRepository = studentRepository;
        this.studentExamRepository = studentExamRepository;
        this.examRepository = examRepository;
        this.claudeClient = claudeClient;
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


    @Tool(description = "CRITICAL TRIGGER: Use this tool IMMEDIATELY if the user mentions an 'uploaded file', 'document', 'PDF', 'screenshot', or if they ask a question about specific course materials, syllabuses, or data that you do not have in your chat memory. Pass a highly descriptive search query into this tool.")
    public String searchDocumentDatabase(String query) {
        System.out.println("🤖 OpenAI Delegated to Claude! Searching Documents for: " + query);

        String response = claudeClient.prompt()
                .user("Search the documents for: " + query + ". Extract the precise facts.")
                .call()
                .content();

        // Add a prefix so OpenAI knows this came from Claude!
        return "CLAUDE'S DOCUMENT ANALYSIS:\n" + response;
    }
}
package org.example.studentdashboard.Tools;

import org.example.studentdashboard.Models.Exam;
import org.example.studentdashboard.Models.Student;
import org.example.studentdashboard.Models.StudentExam;
import org.example.studentdashboard.Repositories.ExamRepository;
import org.example.studentdashboard.Repositories.StudentExamRepository;
import org.example.studentdashboard.Repositories.StudentRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class StudentDashBoardTools {

    StudentRepository studentRepository;
    StudentExamRepository studentExamRepository;
    ExamRepository examRepository;

    public StudentDashBoardTools(StudentRepository studentRepository,StudentExamRepository studentExamRepository,ExamRepository examRepository){
         this.studentRepository = studentRepository;
         this.studentExamRepository = studentExamRepository;
         this.examRepository = examRepository;
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
}

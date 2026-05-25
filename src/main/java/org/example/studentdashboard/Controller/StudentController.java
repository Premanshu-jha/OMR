package org.example.studentdashboard.Controller;

import org.example.studentdashboard.Models.Student;
import org.example.studentdashboard.Models.StudentExam;
import org.example.studentdashboard.Service.StudentService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private StudentService studentService;
    public StudentController(StudentService studentService){
        this.studentService = studentService;
    }

    @GetMapping
    public List<Student> getStudents(@RequestParam Integer pageNumber,@RequestParam Integer pageSize){
        return studentService.getStudents(pageNumber,pageSize);
    }

    @GetMapping("/{id}/report")
    public List<StudentExam> getStudentReport(@PathVariable Long id){
         return studentService.getStudentReport(id);
    }

}

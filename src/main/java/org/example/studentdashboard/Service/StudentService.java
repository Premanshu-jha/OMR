package org.example.studentdashboard.Service;

import org.example.studentdashboard.Models.Student;
import org.example.studentdashboard.Models.StudentExam;
import org.example.studentdashboard.Repositories.StudentRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StudentService {

    private StudentRepository studentRepository;
    public StudentService(StudentRepository studentRepository){
         this.studentRepository = studentRepository;
    }
    public List<Student> getStudents(Integer pageNumber,Integer pageSize){
        Pageable pageReq = PageRequest.of(pageNumber,pageSize);
        return studentRepository.findAll(pageReq).getContent();
    }

    @Transactional
    public List<StudentExam> getStudentReport(Long id){
       Student student = studentRepository.findById(id).orElseThrow(()-> new RuntimeException("Student not found with given id!"));
       return student.getStudentExams();
    }

}

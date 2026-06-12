package org.example.studentdashboard.Service;

import org.example.studentdashboard.Enums.Role;
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
    private JWTService jwtService;
    public StudentService(StudentRepository studentRepository,JWTService jwtService){
         this.studentRepository = studentRepository;
         this.jwtService = jwtService;
    }

    public List<Student> getStudents(String token,Integer pageNumber, Integer pageSize, String rollNumber,
                                     String city, String name,Role role){
        String tokenRole = jwtService.getRole(token);
        if(!"STUDENT".equals(tokenRole)) {
            Pageable pageReq = PageRequest.of(pageNumber, pageSize);
            if (rollNumber != null)
                return studentRepository.findByRollNoContainingIgnoreCase(rollNumber, pageReq).getContent();
            else if (city != null) return studentRepository.findByCityContainingIgnoreCase(city, pageReq).getContent();
            else if (name != null) return studentRepository.findByNameContainingIgnoreCase(name, pageReq).getContent();
            else if (role != null) return studentRepository.findByRole(role, pageReq).getContent();
            return studentRepository.findAll(pageReq).getContent();
        }
        else throw new RuntimeException(" r not authorized to view the student directory!");

    }

    public void postStudent(Student student){
         studentRepository.save(student);
    }

    public void patchStudent(Student student, Long id) {

        Student existingStudent = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No student with the given id exist!"));

        if (student.getRollNo() != null) {
            existingStudent.setRollNo(student.getRollNo());
        }

        if (student.getName() != null) {
            existingStudent.setName(student.getName());
        }

        if (student.getPhone() != null) {
            existingStudent.setPhone(student.getPhone());
        }

        if (student.getCity() != null) {
            existingStudent.setCity(student.getCity());
        }

        if (student.getClassNum() != null) {
            existingStudent.setClassNum(student.getClassNum());
        }

        if (student.getRole() != null) {
            existingStudent.setRole(student.getRole());
        }

        if(student.getSmsOtpByPass() != null){
             existingStudent.setSmsOtpByPass(student.getSmsOtpByPass());
        }

        studentRepository.save(existingStudent);
    }

    @Transactional
    public List<StudentExam> getStudentReport(Long id){
       Student student = studentRepository.findById(id).orElseThrow(()-> new RuntimeException("Student not found with given id!"));
       return student.getStudentExams();
    }


}

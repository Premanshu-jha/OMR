package org.example.studentdashboard.Repositories;

import org.example.studentdashboard.Models.StudentExam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentExamRepository extends JpaRepository<StudentExam,Long> {

    public List<StudentExam> findByStudent_RollNo(String rollNo);

    public Page<StudentExam> findByExam_Id(Long examId, Pageable pageable);

    public Page<StudentExam> findByExam_IdAndStudent_CityContainingIgnoreCase(Long examId,String city,Pageable pageable);

    public Page<StudentExam> findByExam_IdAndStudent_RollNoContainingIgnoreCase(Long examId,String rollNo,Pageable pageable);

    public Page<StudentExam> findByExam_IdAndStudent_NameContainingIgnoreCase(Long examId,String name,Pageable pageable);


}

package org.example.studentdashboard.Repositories;

import org.example.studentdashboard.Models.StudentExam;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentExamRepository extends JpaRepository<StudentExam,Long> {
}

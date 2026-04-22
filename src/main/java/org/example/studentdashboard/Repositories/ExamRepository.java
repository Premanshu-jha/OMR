package org.example.studentdashboard.Repositories;

import org.example.studentdashboard.Models.Exam;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExamRepository extends JpaRepository<Exam,Long> {

    public Optional<Exam> findByExamIdentifier(String examIdentifier);
}

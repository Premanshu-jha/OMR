package org.example.studentdashboard.Repositories;

import org.example.studentdashboard.Models.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student,Long> {

    public Optional<Student> findByRollNo(String rollNo);
}

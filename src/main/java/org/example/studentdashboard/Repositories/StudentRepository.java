package org.example.studentdashboard.Repositories;

import org.example.studentdashboard.Models.Student;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student,Long> {

}

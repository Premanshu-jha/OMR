package org.example.studentdashboard.Repositories;

import org.example.studentdashboard.Enums.Role;
import org.example.studentdashboard.Models.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student,Long> {

    public Optional<Student> findByRollNo(String rollNo);

    public List<Student> findByNameContainingIgnoreCase(String name);

    public Page<Student> findByRollNoContainingIgnoreCase(String rollNo,Pageable pageable);

    public Page<Student> findByNameContainingIgnoreCase(String name, Pageable pageable);

    public Page<Student> findByCityContainingIgnoreCase(String city,Pageable pageable);

    public Page<Student> findByRole(Role role,Pageable pageable);

}

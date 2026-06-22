package org.example.studentdashboard.Repositories;

import org.example.studentdashboard.Models.StudentExam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StudentExamRepository extends JpaRepository<StudentExam,Long> {

    public List<StudentExam> findByStudent_RollNo(String rollNo);

    public Page<StudentExam> findByExam_Id(Long examId, Pageable pageable);

    public Page<StudentExam> findByExam_IdAndStudent_CityContainingIgnoreCase(Long examId,String city,Pageable pageable);

    public Page<StudentExam> findByExam_IdAndStudent_RollNoContainingIgnoreCase(Long examId,String rollNo,Pageable pageable);

    public Page<StudentExam> findByExam_IdAndStudent_NameContainingIgnoreCase(Long examId,String name,Pageable pageable);

    public List<StudentExam> findByExam_ExamIdentifier(String examIdentifier);

    @Query("""
    SELECT se FROM StudentExam se 
    WHERE (:examId IS NULL OR se.exam.examIdentifier = :examId) 
    ORDER BY se.physicsRank ASC
""")
    public List<StudentExam> findTopByPhysicsRank(@Param("examId") String examId, Pageable pageable);

    @Query("""
    SELECT se FROM StudentExam se 
    WHERE (:examId IS NULL OR se.exam.examIdentifier = :examId) 
    ORDER BY se.mathsRank ASC
""")
    public List<StudentExam> findTopByMathsRank(@Param("examId") String examId, Pageable pageable);

    @Query("""
    SELECT se FROM StudentExam se 
    WHERE (:examId IS NULL OR se.exam.examIdentifier = :examId) 
    ORDER BY se.chemistryRank ASC
""")
    public List<StudentExam> findTopByChemistryRank(@Param("examId") String examId, Pageable pageable);

    @Query("""
    SELECT se FROM StudentExam se 
    WHERE (:examId IS NULL OR se.exam.examIdentifier = :examId) 
    ORDER BY se.rank ASC
""")
    public List<StudentExam> findTopRankers(@Param("examId") String examId, Pageable pageable);
}

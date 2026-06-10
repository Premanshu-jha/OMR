package org.example.studentdashboard.Service;

import org.example.studentdashboard.Models.Exam;
import org.example.studentdashboard.Models.StudentExam;
import org.example.studentdashboard.Repositories.ExamRepository;
import org.example.studentdashboard.Repositories.StudentExamRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ExamService {

    private ExamRepository examRepository;
    private StudentExamRepository studentExamRepository;

    public ExamService(ExamRepository examRepository,StudentExamRepository studentExamRepository){
         this.examRepository = examRepository;
         this.studentExamRepository = studentExamRepository;
    }

    @Transactional
    public List<StudentExam> getExamLeaderBoard(Long id,Integer pageNumber,Integer pageSize){
          Exam exam = examRepository.findById(id)
                  .orElseThrow(()->new RuntimeException("No exam with given id found!"));
        Pageable pageReq = PageRequest.of(pageNumber,pageSize, Sort.by("rank"));
          return studentExamRepository.findByExam_Id(id,pageReq).getContent();
    }

    public List<Exam> getExams(String type){
        if(type != null) return examRepository.findByExamType(type);
         return examRepository.findAll();
    }

}

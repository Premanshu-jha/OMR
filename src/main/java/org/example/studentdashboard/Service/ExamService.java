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
    public List<StudentExam> getExamLeaderBoard(Long examId,Integer pageNumber,Integer pageSize,String city,
      String rollNo,String name){

        Pageable pageReq = PageRequest.of(pageNumber,pageSize, Sort.by("rank"));
        if(rollNo != null) {
            return studentExamRepository
                    .findByExam_IdAndStudent_RollNoContainingIgnoreCase(examId, rollNo, pageReq).getContent();
        }
        else if(city != null) {
            return studentExamRepository
                    .findByExam_IdAndStudent_CityContainingIgnoreCase(examId, city, pageReq).getContent();
        }
        else if(name != null) {
            return studentExamRepository
                    .findByExam_IdAndStudent_NameContainingIgnoreCase(examId, name, pageReq).getContent();

        }
        return studentExamRepository.findByExam_Id(examId,pageReq).getContent();
    }

    public List<Exam> getExams(String type,String examIdentifier){
        if(type != null && examIdentifier == null) return examRepository.findByExamType(type);

        else if(examIdentifier != null && type == null)
            return examRepository.findByExamIdentifierContainingIgnoreCase(examIdentifier);

        else if(type != null && examIdentifier != null)
            return examRepository.findByExamTypeAndExamIdentifierContainingIgnoreCase(type,examIdentifier);

         return examRepository.findAll();
    }

}

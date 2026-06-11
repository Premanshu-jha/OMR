package org.example.studentdashboard.Controller;

import org.example.studentdashboard.Models.Exam;
import org.example.studentdashboard.Models.StudentExam;
import org.example.studentdashboard.Service.ExamService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exams")
public class ExamController {

    private ExamService examService;

    public ExamController(ExamService examService){
        this.examService = examService;
    }

    @GetMapping
    public List<Exam> getExams(@RequestParam(required = false) String type){
        return examService.getExams(type);
    }

    @GetMapping("/{examId}")
    public List<StudentExam> getExamLeaderBoard(@PathVariable  Long examId,
                                                @RequestParam  Integer pageNumber,
                                                @RequestParam  Integer pageSize,
                                                @RequestParam(required = false) String city,
                                                @RequestParam(required = false) String rollNo,
                                                @RequestParam(required = false) String name){

      return examService.getExamLeaderBoard(examId,pageNumber,pageSize,city,rollNo,name);
    }

}

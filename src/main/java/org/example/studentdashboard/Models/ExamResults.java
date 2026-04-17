package org.example.studentdashboard.Models;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExamResults {

    private String examId;
    private Integer totalStudentsAttempted;

    private Integer physicsTotalMarks;
    private Integer physicsTotalQuestions;

    private Integer mathsTotalMarks;
    private Integer mathsTotalQuestions;

    private Integer chemistryTotalMarks;
    private Integer chemistryTotalQuestions;

    private Integer examTotalMarks;
    private Integer examTotalQuestions;

    private List<StudentData> studentData;
}

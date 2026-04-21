package org.example.studentdashboard.Models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(indexes = {
        @Index(name = "student_index", columnList = "student_id"),
@Index(name = "exam_index",columnList = "exam_id")
}
)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
public class StudentExam {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE,generator = "studentExamSequenceGenerator")
    @SequenceGenerator(name = "studentExamSequenceGenerator",
            sequenceName = "student_exam_sequence",allocationSize = 100)
    @EqualsAndHashCode.Include
    private Long id;


    @ManyToOne
    @JoinColumn(name = "student_id")
    private Student student;

    @ManyToOne
    @JoinColumn(name = "exam_id")
    private Exam exam;

    private Integer physicsAttemptedQuestions;
    private Integer physicsCorrectAnswers;
    private Integer physicsWrongAnswers;
    private Integer physicsPositiveMarks;
    private Integer physicsNegativeMarks;
    private Integer physicsMarksScored;
    private String physicsTotalTimeSpent;
    private String physicsAvgTimeEachQuestion;
    private Long physicsRank;

    private Integer mathsAttemptedQuestions;
    private Integer mathsCorrectAnswers;
    private Integer mathsWrongAnswers;
    private Integer mathsPositiveMarks;
    private Integer mathsNegativeMarks;
    private Integer mathsMarksScored;
    private String mathsTotalTimeSpent;
    private String mathsAvgTimeEachQuestion;
    private Long mathsRank;

    private Integer chemistryAttemptedQuestions;
    private Integer chemistryCorrectAnswers;
    private Integer chemistryWrongAnswers;
    private Integer chemistryPositiveMarks;
    private Integer chemistryNegativeMarks;
    private Integer chemistryMarksScored;
    private String chemistryTotalTimeSpent;
    private String chemistryAvgTimeEachQuestion;
    private Long chemistryRank;

    private Integer totalAttemptedQuestions;
    private Integer totalCorrectAnswers;
    private Integer totalWrongAnswers;
    private Integer totalPositiveMarks;
    private Integer totalNegativeMarks;
    private Integer totalMarks;
    private String totalTimeSpent;
    private String avgTimeEachQuestion;
    private Long rank;
    private String timeOutside;
    private String examStartTime;
    private String examEndTime;




}

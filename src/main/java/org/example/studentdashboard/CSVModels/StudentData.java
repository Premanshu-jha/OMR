package org.example.studentdashboard.CSVModels;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class StudentData {

    private String rollNo;
    private String name;
    private String center;
    private String phone;
    private String city;
    private Integer classNum;

    // MATHS
    private Integer mathsTotalQuestions;
    private Integer mathsAttemptedQuestions;
    private Integer mathsCorrectAnswers;
    private Integer mathsWrongAnswers;
    private Integer mathsPositiveMarks;
    private Integer mathsNegativeMarks;
    private Integer mathsMarksScored;
    private String mathsTotalTimeSpent;
    private String mathsAvgTimeEachQuestion;
    private Long mathsRank;

    // PHYSICS
    private Integer physicsTotalQuestions;
    private Integer physicsAttemptedQuestions;
    private Integer physicsCorrectAnswers;
    private Integer physicsWrongAnswers;
    private Integer physicsPositiveMarks;
    private Integer physicsNegativeMarks;
    private Integer physicsMarksScored;
    private String physicsTotalTimeSpent;
    private String physicsAvgTimeEachQuestion;
    private Long physicsRank;

    // CHEMISTRY
    private Integer chemistryTotalQuestions;
    private Integer chemistryAttemptedQuestions;
    private Integer chemistryCorrectAnswers;
    private Integer chemistryWrongAnswers;
    private Integer chemistryPositiveMarks;
    private Integer chemistryNegativeMarks;
    private Integer chemistryMarksScored;
    private String chemistryTotalTimeSpent;
    private String chemistryAvgTimeEachQuestion;
    private Long chemistryRank;

    // TOTAL
    private Integer totalQuestions;
    private Integer totalAttemptedQuestions;
    private Integer totalCorrectAnswers;
    private Integer totalWrongAnswers;
    private Integer totalPositiveMarks;
    private Integer totalNegativeMarks;
    private Integer totalMarks;
    private String totalTimeSpent;
    private String avgTimeEachQuestion;
    private Long rank;

    // Other
    private Integer questionsIncorrect;
    private Integer questionsNotAttempted;

    // Nullable - present in some CSV formats
    private String timeOutside;
    private String examStartTime;
    private String examEndTime;
}
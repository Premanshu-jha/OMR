package org.example.studentdashboard.Models;
import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvBindByPosition;
import lombok.Data;

@Data
public class StudentData {

    @CsvBindByName(column = "# ID NO")
    private String studentId;

    @CsvBindByName(column = "STUDENT NAME")
    private String name;

    @CsvBindByName(column = "CENTRE")
    private String center;

    @CsvBindByName(column = "PHONE")
    private String phone;

    @CsvBindByName(column = "CITY")
    private String city;

    @CsvBindByName(column = "CLASS")
    private Integer classNum;

    @CsvBindByName(column = "PHYSICS -- Total Questions")
    private Integer physicsTotalQuestions;

    @CsvBindByName(column = "PHYSICS -- Attempted Questions")
    private Integer physicsAttemptedQuestions;

    @CsvBindByName(column = "PHYSICS -- Correct Answers")
    private Integer physicsCorrectAnswers;

    @CsvBindByName(column = "PHYSICS -- Wrong Answers")
    private Integer physicsWrongAnswers;

    @CsvBindByName(column = "PHYSICS -- Positive Marks")
    private Integer physicsPositiveMarks;

    @CsvBindByName(column = "PHYSICS -- Negative Marks")
    private Integer physicsNegativeMarks;

    @CsvBindByName(column = "PHYSICS -- 66 Total Marks")
    private Integer physicsTotalMarks;

    @CsvBindByName(column = "PHYSICS -- Total Time Spent")
    private String physicsTotalTimeSpent;

    @CsvBindByName(column = "PHYSICS -- Avg Time on each Question")
    private String physicsAvgTimeEachQuestion;

    @CsvBindByPosition(position = 15)
    private Integer physicsRank;

    @CsvBindByName(column = "MATHS -- Total Questions")
    private Integer mathsTotalQuestions;

    @CsvBindByName(column = "MATHS -- Attempted Questions")
    private Integer mathsAttemptedQuestions;

    @CsvBindByName(column = "MATHS -- Correct Answers")
    private Integer mathsCorrectAnswers;

    @CsvBindByName(column = "MATHS -- Wrong Answers")
    private Integer mathsWrongAnswers;

    @CsvBindByName(column = "MATHS -- Positive Marks")
    private Integer mathsPositiveMarks;

    @CsvBindByName(column = "MATHS -- Negative Marks")
    private Integer mathsNegativeMarks;

    @CsvBindByName(column = "MATHS -- 66 Total Marks")
    private Integer mathsTotalMarks;

    @CsvBindByName(column = "MATHS -- Total Time Spent")
    private String mathsTotalTimeSpent;

    @CsvBindByName(column = "MATHS -- Avg Time on each Question")
    private String mathsAvgTimeEachQuestion;

    @CsvBindByPosition(position = 25)
    private Integer mathsRank;

    @CsvBindByName(column = "CHEMISTRY -- Total Questions")
    private Integer chemistryTotalQuestions;

    @CsvBindByName(column = "CHEMISTRY -- Attempted Questions")
    private Integer chemistryAttemptedQuestions;

    @CsvBindByName(column = "CHEMISTRY -- Correct Answers")
    private Integer chemistryCorrectAnswers;

    @CsvBindByName(column = "CHEMISTRY -- Wrong Answers")
    private Integer chemistryWrongAnswers;

    @CsvBindByName(column = "CHEMISTRY -- Positive Marks")
    private Integer chemistryPositiveMarks;

    @CsvBindByName(column = "CHEMISTRY -- Negative Marks")
    private Integer chemistryNegativeMarks;

    @CsvBindByName(column = "CHEMISTRY -- 66 Total Marks")
    private Integer chemistryTotalMarks;

    @CsvBindByName(column = "CHEMISTRY -- Total Time Spent")
    private String chemistryTotalTimeSpent;

    @CsvBindByName(column = "CHEMISTRY -- Avg Time on each Question")
    private String chemistryAvgTimeEachQuestion;

    @CsvBindByPosition(position = 35)
    private Integer chemistryRank;

    @CsvBindByName(column = "TOTAL -- Total Questions")
    private Integer totalQuestions;

    @CsvBindByName(column = "TOTAL -- Attempted Questions")
    private Integer totalAttemptedQuestions;

    @CsvBindByName(column = "TOTAL -- Correct Answers")
    private Integer totalCorrectAnswers;

    @CsvBindByName(column = "TOTAL -- Wrong Answers")
    private Integer totalWrongAnswers;

    @CsvBindByName(column = "TOTAL -- Positive Marks")
    private Integer totalPositiveMarks;

    @CsvBindByName(column = "TOTAL -- Negative Marks")
    private Integer totalNegativeMarks;

    @CsvBindByName(column = "TOTAL -- 198 Total Marks")
    private Integer totalMarks;

    @CsvBindByName(column = "TOTAL -- Total Time Spent")
    private String totalTimeSpent;

    @CsvBindByName(column = "TOTAL -- Avg Time on each Question")
    private String avgTimeEachQuestion;

    @CsvBindByPosition(position = 45)
    private Integer rank;

    @CsvBindByName(column = "TIME OUTSIDE")
    private String timeOutside;

    @CsvBindByName(column = "EXAM STARTED AT")
    private String examStartTime;

    @CsvBindByName(column = "EXAM ENDED AT")
    private String examEndTime;

    @CsvBindByName(column = "Qs Incorrect")
    private Integer questionsIncorrect;

    @CsvBindByName(column = "Qs Not Attempted")
    private Integer questionsNotAttempted;
}

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
    private List<StudentData> studentData;
}

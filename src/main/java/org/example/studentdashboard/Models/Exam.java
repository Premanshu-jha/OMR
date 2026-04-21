package org.example.studentdashboard.Models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@Table(indexes = {
        @Index(name = "exam_index",columnList = "exam_id")
})
public class Exam {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE,generator = "examSequenceGenerator")
    @SequenceGenerator(name = "examSequenceGenerator",sequenceName = "exam_sequence",
    allocationSize = 100)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "exam_id", unique = true,nullable = false)
    private String examId;

    private Integer examTotalMarks;

    private Integer physicsTotalMarks;

    private Integer mathsTotalMarks;

    private Integer chemistryTotalMarks;

    private Integer totalStudentsAttempted;

    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY,mappedBy = "exam")
    @JsonIgnore
    private List<StudentExam> studentExams;


}

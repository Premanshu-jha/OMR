package org.example.studentdashboard.Models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@Table(indexes = {
        @Index(name = "exam_identifier_index",columnList = "exam_identifier"),
        @Index(name = "exam_type_index",columnList = "exam_type")
})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Exam {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE,generator = "examSequenceGenerator")
    @SequenceGenerator(name = "examSequenceGenerator",sequenceName = "exam_sequence",
    allocationSize = 100)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "exam_identifier", unique = true,nullable = false)
    private String examIdentifier;

    private Integer examTotalMarks;

    private Integer physicsTotalMarks;

    private Integer mathsTotalMarks;

    private Integer chemistryTotalMarks;

    private Integer totalStudentsAttempted;

    @Column(name = "exam_type")
    private String examType;

    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY,mappedBy = "exam")
    @JsonIgnore
    private List<StudentExam> studentExams;


}

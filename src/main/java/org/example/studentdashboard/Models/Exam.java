package org.example.studentdashboard.Models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(indexes = {
        @Index(name = "exam_id_index",columnList = "exam_id")
})
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Exam{

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE,generator = "exam_seq_generator")
    @SequenceGenerator(name = "exam_seq_generator",sequenceName = "exam_seq",allocationSize = 1)
    private Long id;

    @Column(unique = true,name = "exam_id",nullable = false)
    private String examId;

    private Integer totalMarks;

    private Integer totalStudentsAttempted;

}

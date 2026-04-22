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
        @Index(name = "roll_index",columnList = "roll_no")
})
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE,generator = "studentSequenceGenerator")
    @SequenceGenerator(name = "studentSequenceGenerator",sequenceName = "student_sequence",
    allocationSize = 100)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "roll_no",unique = true,nullable = false)
    private String rollNo;

    private String name;

    private String phone;

    private String city;

    private Integer classNum;

    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY,mappedBy = "student")
    @JsonIgnore
    private List<StudentExam> studentExams;
}

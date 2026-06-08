package org.example.studentdashboard.Models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.example.studentdashboard.Enums.Role;

import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@Table(indexes = {
        @Index(name = "roll_index",columnList = "roll_no"),
        @Index(name = "name_index",columnList = "name")
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

    @Column(name = "name")
    private String name;

    private String phone;

    private String city;

    private Integer classNum;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY,mappedBy = "student")
    @JsonIgnore
    private List<StudentExam> studentExams;

}

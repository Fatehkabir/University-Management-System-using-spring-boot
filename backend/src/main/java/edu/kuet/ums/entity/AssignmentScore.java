package edu.kuet.ums.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "assignment_scores",
       uniqueConstraints = @UniqueConstraint(name = "uk_assignment_student", columnNames = {"assignment_id", "student_id"}))
@Getter
@Setter
@NoArgsConstructor
public class AssignmentScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @NotNull
    @DecimalMin("0.0")
    @Column(name = "obtained_marks", nullable = false, precision = 6, scale = 2)
    private BigDecimal obtainedMarks;

    @Size(max = 255)
    @Column(length = 255)
    private String feedback;

    @Column(name = "graded_at", nullable = false)
    private Instant gradedAt = Instant.now();

    public AssignmentScore(Assignment assignment, Student student, BigDecimal obtainedMarks, String feedback) {
        this.assignment = assignment;
        this.student = student;
        this.obtainedMarks = obtainedMarks;
        this.feedback = feedback;
        this.gradedAt = Instant.now();
    }
}


package edu.kuet.ums.entity;

import edu.kuet.ums.entity.enums.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "students")
@DiscriminatorValue("STUDENT")
@Getter
@Setter
@NoArgsConstructor
public class Student extends User {

    @Column(name = "student_id", nullable = false, unique = true, length = 30)
    private String studentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(length = 20)
    private String batch;

    @Column(nullable = false)
    private int semester = 1;

    public Student(String username, String password, String email, String fullName, String phone,
                   String address, String studentId, Department department, String batch, int semester) {
        super(username, password, email, fullName, phone, address);
        this.studentId = studentId;
        this.department = department;
        this.batch = batch;
        this.semester = semester;
    }

    @Override
    public Role getRole() {
        return Role.STUDENT;
    }
}


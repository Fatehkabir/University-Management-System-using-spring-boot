package edu.kuet.ums.entity;

import edu.kuet.ums.entity.enums.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "teachers")
@DiscriminatorValue("TEACHER")
@Getter
@Setter
@NoArgsConstructor
public class Teacher extends User {

    @Column(name = "employee_id", nullable = false, unique = true, length = 30)
    private String employeeId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(length = 80)
    private String designation;

    @Column(length = 150)
    private String qualification;

    @Column(name = "joining_date")
    private LocalDate joiningDate;

    public Teacher(String username, String password, String email, String fullName, String phone,
                   String address, String employeeId, Department department, String designation,
                   String qualification, LocalDate joiningDate) {
        super(username, password, email, fullName, phone, address);
        this.employeeId = employeeId;
        this.department = department;
        this.designation = designation;
        this.qualification = qualification;
        this.joiningDate = joiningDate;
    }

    @Override
    public Role getRole() {
        return Role.TEACHER;
    }
}


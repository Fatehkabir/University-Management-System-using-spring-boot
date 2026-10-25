package edu.kuet.ums.entity;

import edu.kuet.ums.entity.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "admins")
@DiscriminatorValue("ADMIN")
@Getter
@Setter
@NoArgsConstructor
public class Admin extends User {

    @Column(name = "staff_code", length = 30)
    private String staffCode;

    public Admin(String username, String password, String email, String fullName,
                 String phone, String address, String staffCode) {
        super(username, password, email, fullName, phone, address);
        this.staffCode = staffCode;
    }

    @Override
    public Role getRole() {
        return Role.ADMIN;
    }
}


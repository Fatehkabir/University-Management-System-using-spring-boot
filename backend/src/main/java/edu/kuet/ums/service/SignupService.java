package edu.kuet.ums.service;

import edu.kuet.ums.dto.Requests.RegisterRequest;
import edu.kuet.ums.dto.Requests.SignupRole;
import edu.kuet.ums.dto.Requests.StudentRequest;
import edu.kuet.ums.dto.Requests.TeacherRequest;
import edu.kuet.ums.dto.Responses.RegisterResponse;
import edu.kuet.ums.dto.Responses.SignupInfo;
import edu.kuet.ums.exception.ApiExceptions.FieldValidationException;
import edu.kuet.ums.exception.ApiExceptions.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class SignupService {

    private final TeacherService teachers;
    private final StudentService students;
    private final DepartmentService departments;
    private final boolean enabled;
    private final boolean requireApproval;

    public SignupService(TeacherService teachers, StudentService students, DepartmentService departments,
                         @Value("${app.signup.enabled:true}") boolean enabled,
                         @Value("${app.signup.require-approval:false}") boolean requireApproval) {
        this.teachers = teachers;
        this.students = students;
        this.departments = departments;
        this.enabled = enabled;
        this.requireApproval = requireApproval;
    }

    public SignupInfo info() {
        return new SignupInfo(enabled, requireApproval, enabled ? departments.findAll() : java.util.List.of());
    }

    @Transactional
    public RegisterResponse register(RegisterRequest r) {
        if (!enabled) {
            throw new ForbiddenException("Self sign-up is turned off. Please ask an administrator for an account.");
        }
        boolean active = !requireApproval;

        if (r.role() == SignupRole.TEACHER) {
            requireText(r.employeeId(), "employeeId", "Employee ID is required for teachers");
            teachers.create(new TeacherRequest(r.username().trim(), r.password(), r.email().trim(), r.fullName().trim(),
                    r.phone(), r.address(), r.employeeId().trim(), r.departmentId(),
                    r.designation(), r.qualification(), r.joiningDate(), active));
        } else {
            requireText(r.studentId(), "studentId", "Student ID is required for students");
            students.create(new StudentRequest(r.username().trim(), r.password(), r.email().trim(), r.fullName().trim(),
                    r.phone(), r.address(), r.studentId().trim(), r.departmentId(),
                    r.batch(), r.semester(), active));
        }
        return new RegisterResponse(requireApproval, requireApproval
                ? "Account created. An administrator must approve it before you can sign in."
                : "Account created. You can sign in now.");
    }

    private static void requireText(String value, String field, String message) {
        if (value == null || value.isBlank()) {
            Map<String, String> errors = new LinkedHashMap<>();
            errors.put(field, message);
            throw new FieldValidationException(errors);
        }
    }
}


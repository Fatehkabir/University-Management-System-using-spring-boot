package edu.kuet.ums.dto;

import edu.kuet.ums.entity.enums.AttendanceStatus;
import edu.kuet.ums.entity.enums.EnrollmentStatus;
import edu.kuet.ums.entity.enums.Role;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public final class Requests {

    private Requests() {
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    public record ChangePasswordRequest(@NotBlank String currentPassword,
                                        @NotBlank @Size(min = 6, max = 100) String newPassword) {
    }

    public record DepartmentRequest(@NotBlank @Size(max = 120) String name,
                                    @NotBlank @Size(max = 20) String code) {
    }

    public record TeacherRequest(
            @NotBlank @Size(max = 60) String username,
            @Size(min = 6, max = 100) String password,
            @NotBlank @Email @Size(max = 150) String email,
            @NotBlank @Size(max = 150) String fullName,
            @Size(max = 20) String phone,
            @Size(max = 255) String address,
            @NotBlank @Size(max = 30) String employeeId,
            @NotNull Long departmentId,
            @Size(max = 80) String designation,
            @Size(max = 150) String qualification,
            LocalDate joiningDate,
            Boolean enabled) {
    }

    public record StudentRequest(
            @NotBlank @Size(max = 60) String username,
            @Size(min = 6, max = 100) String password,
            @NotBlank @Email @Size(max = 150) String email,
            @NotBlank @Size(max = 150) String fullName,
            @Size(max = 20) String phone,
            @Size(max = 255) String address,
            @NotBlank @Size(max = 30) String studentId,
            @NotNull Long departmentId,
            @Size(max = 20) String batch,
            @Min(1) @Max(12) Integer semester,
            Boolean enabled) {
    }

    public record CourseRequest(
            @NotBlank @Size(max = 20) String code,
            @NotBlank @Size(max = 150) String title,
            @NotNull @DecimalMin("0.5") @DecimalMax("12") Double creditHours,
            @NotNull Long departmentId,
            Long teacherId) {
    }

    public record EnrollRequest(@NotNull Long studentId, @NotNull Long courseId) {
    }

    public record SelfEnrollRequest(@NotNull Long courseId) {
    }

    public record GradeRequest(
            @Pattern(regexp = "^$|^(A\\+|A|A-|B\\+|B|B-|C\\+|C|C-|D|F)$",
                    message = "Grade must be one of A+, A, A-, B+, B, B-, C+, C, C-, D, F")
            String grade,
            EnrollmentStatus status) {
    }

    public record AttendanceEntry(@NotNull Long studentId, @NotNull AttendanceStatus status) {
    }

    public record AttendanceRequest(@NotNull LocalDate date,
                                    @NotEmpty @Valid List<AttendanceEntry> entries) {
    }

    public enum SignupRole {
        TEACHER, STUDENT
    }

    public record RegisterRequest(
            @NotNull SignupRole role,
            @NotBlank @Size(min = 3, max = 60)
            @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "Use letters, numbers, dot, dash or underscore only")
            String username,
            @NotBlank @Size(min = 6, max = 100) String password,
            @NotBlank @Email @Size(max = 150) String email,
            @NotBlank @Size(max = 150) String fullName,
            @Size(max = 20) String phone,
            @Size(max = 255) String address,
            @NotNull Long departmentId,
            @Size(max = 30) String employeeId,
            @Size(max = 80) String designation,
            @Size(max = 150) String qualification,
            LocalDate joiningDate,
            @Size(max = 30) String studentId,
            @Size(max = 20) String batch,
            @Min(1) @Max(12) Integer semester) {
    }

    public record ConvertToStudentRequest(
            @NotBlank @Size(max = 30) String studentId,
            @Size(max = 20) String batch,
            @Min(1) @Max(12) Integer semester,
            Long departmentId) {
    }

    public record ConvertToTeacherRequest(
            @NotBlank @Size(max = 30) String employeeId,
            @Size(max = 80) String designation,
            @Size(max = 150) String qualification,
            LocalDate joiningDate,
            Long departmentId,
            Boolean deleteRecords) {
    }

    public record AnnouncementRequest(@NotBlank @Size(max = 150) String title,
                                      @NotBlank String content,
                                      Role targetRole) {
    }

    public record AssignmentRequest(@NotBlank @Size(max = 120) String title,
                                    String description,
                                    @NotNull @DecimalMin("0.0") java.math.BigDecimal maxMarks,
                                    LocalDate dueDate) {
    }

    public record AssignmentScoreRequest(@NotNull Long studentId,
                                         @NotNull @DecimalMin("0.0") java.math.BigDecimal obtainedMarks,
                                         @Size(max = 255) String feedback) {
    }
}


package edu.kuet.ums.dto;

import edu.kuet.ums.entity.enums.AttendanceStatus;
import edu.kuet.ums.entity.enums.EnrollmentStatus;
import edu.kuet.ums.entity.enums.Role;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class Responses {

    private Responses() {
    }

    public record UserSummary(Long id, String username, String fullName, Role role) {
    }

    public record AuthResponse(String token, String tokenType, long expiresInMs, UserSummary user) {
    }

    public record DepartmentResponse(Long id, String name, String code) {
    }

    public record TeacherResponse(Long id, String username, String email, String fullName, String phone,
                                  String address, boolean enabled, String employeeId, Long departmentId,
                                  String departmentName, String designation, String qualification,
                                  LocalDate joiningDate) {
    }

    public record StudentResponse(Long id, String username, String email, String fullName, String phone,
                                  String address, boolean enabled, String studentId, Long departmentId,
                                  String departmentName, String batch, int semester) {
    }

    public record CourseResponse(Long id, String code, String title, double creditHours, Long departmentId,
                                 String departmentName, Long teacherId, String teacherName) {
    }

    public record EnrollmentResponse(Long id, Long studentId, String studentName, String studentCode,
                                     Long courseId, String courseCode, String courseTitle,
                                     String teacherName, EnrollmentStatus status, String grade,
                                     LocalDateTime enrolledAt, double creditHours) {
    }

    public record AttendanceResponse(Long id, Long studentId, String studentName, Long courseId,
                                     String courseCode, LocalDate date, AttendanceStatus status) {
    }

    public record AnnouncementResponse(Long id, String title, String content, Role targetRole,
                                       String postedByName, LocalDateTime createdAt) {
    }

    public record NameCount(String name, long count) {
    }

    public record CourseCount(String code, String title, long count) {
    }

    public record StatsResponse(long teachers, long students, long courses, long departments,
                                long enrollments, long activeEnrollments, long completedEnrollments,
                                long droppedEnrollments, long coursesWithoutTeacher, long disabledAccounts,
                                Double attendanceRate, List<NameCount> studentsByDepartment,
                                List<CourseCount> topCourses) {
    }

    public record TeacherCourseStat(Long courseId, String code, String title, double creditHours,
                                    long students, long graded, Double attendanceRate) {
    }

    public record TeacherStatsResponse(long courses, long students, long graded, Double attendanceRate,
                                       List<TeacherCourseStat> courseStats) {
    }

    public record SignupInfo(boolean enabled, boolean requireApproval, List<DepartmentResponse> departments) {
    }

    public record RegisterResponse(boolean approvalRequired, String message) {
    }

    public record AssignmentResponse(Long id, Long courseId, String courseCode, String courseTitle,
                                     String title, String description, java.math.BigDecimal maxMarks,
                                     java.time.LocalDate dueDate, java.time.Instant createdAt,
                                     long submissionsCount) {
    }

    public record AssignmentScoreResponse(Long id, Long assignmentId, Long studentId,
                                          String studentName, String studentCode,
                                          java.math.BigDecimal obtainedMarks, java.math.BigDecimal maxMarks,
                                          String feedback, java.time.Instant gradedAt) {
    }

    public record StudentAssignmentItem(Long assignmentId, Long courseId, String courseCode, String courseTitle,
                                        String title, String description, java.math.BigDecimal maxMarks,
                                        java.time.LocalDate dueDate, java.math.BigDecimal obtainedMarks,
                                        String feedback, java.time.Instant gradedAt) {
    }
}


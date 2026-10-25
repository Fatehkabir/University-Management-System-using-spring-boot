package edu.kuet.ums.controller;

import edu.kuet.ums.dto.Requests.AssignmentRequest;
import edu.kuet.ums.dto.Requests.AssignmentScoreRequest;
import edu.kuet.ums.dto.Requests.AttendanceRequest;
import edu.kuet.ums.dto.Requests.GradeRequest;
import edu.kuet.ums.dto.Responses.AssignmentResponse;
import edu.kuet.ums.dto.Responses.AssignmentScoreResponse;
import edu.kuet.ums.dto.Responses.AttendanceResponse;
import edu.kuet.ums.dto.Responses.CourseResponse;
import edu.kuet.ums.dto.Responses.EnrollmentResponse;
import edu.kuet.ums.dto.Responses.TeacherResponse;
import edu.kuet.ums.dto.Responses.TeacherStatsResponse;
import edu.kuet.ums.service.AssignmentService;
import edu.kuet.ums.service.AttendanceService;
import edu.kuet.ums.service.CourseService;
import edu.kuet.ums.service.EnrollmentService;
import edu.kuet.ums.service.StatsService;
import edu.kuet.ums.service.TeacherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/teacher")
@RequiredArgsConstructor
public class TeacherController {

    private final TeacherService teachers;
    private final CourseService courses;
    private final EnrollmentService enrollments;
    private final AttendanceService attendance;
    private final AssignmentService assignments;
    private final StatsService stats;

    @GetMapping("/profile")
    public TeacherResponse profile(Authentication auth) {
        return teachers.profile(auth.getName());
    }

    @GetMapping("/stats")
    public TeacherStatsResponse stats(Authentication auth) {
        return stats.teacherStats(auth.getName());
    }

    @GetMapping("/courses")
    public List<CourseResponse> myCourses(Authentication auth) {
        return courses.findByTeacher(auth.getName());
    }

    @GetMapping("/courses/{courseId}/students")
    public List<EnrollmentResponse> roster(Authentication auth, @PathVariable Long courseId) {
        return enrollments.roster(auth.getName(), courseId);
    }

    @PutMapping("/enrollments/{enrollmentId}/grade")
    public EnrollmentResponse grade(Authentication auth, @PathVariable Long enrollmentId,
                                    @Valid @RequestBody GradeRequest request) {
        return enrollments.grade(auth.getName(), enrollmentId, request);
    }

    @GetMapping("/courses/{courseId}/attendance")
    public List<AttendanceResponse> attendance(Authentication auth, @PathVariable Long courseId,
                                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return attendance.forCourseOnDate(auth.getName(), courseId, date);
    }

    @PostMapping("/courses/{courseId}/attendance")
    public List<AttendanceResponse> recordAttendance(Authentication auth, @PathVariable Long courseId,
                                                     @Valid @RequestBody AttendanceRequest request) {
        return attendance.record(auth.getName(), courseId, request);
    }

    @GetMapping("/courses/{courseId}/assignments")
    public List<AssignmentResponse> assignments(Authentication auth, @PathVariable Long courseId) {
        return assignments.getAssignmentsForTeacher(auth.getName(), courseId);
    }

    @PostMapping("/courses/{courseId}/assignments")
    public AssignmentResponse createAssignment(Authentication auth, @PathVariable Long courseId,
                                               @Valid @RequestBody AssignmentRequest request) {
        return assignments.createAssignment(auth.getName(), courseId, request);
    }

    @DeleteMapping("/courses/{courseId}/assignments/{assignmentId}")
    public void deleteAssignment(Authentication auth, @PathVariable Long courseId,
                                 @PathVariable Long assignmentId) {
        assignments.deleteAssignment(auth.getName(), courseId, assignmentId);
    }

    @GetMapping("/courses/{courseId}/assignments/{assignmentId}/scores")
    public List<AssignmentScoreResponse> assignmentScores(Authentication auth, @PathVariable Long courseId,
                                                          @PathVariable Long assignmentId) {
        return assignments.getAssignmentScores(auth.getName(), courseId, assignmentId);
    }

    @PutMapping("/courses/{courseId}/assignments/{assignmentId}/scores")
    public AssignmentScoreResponse saveScore(Authentication auth, @PathVariable Long courseId,
                                            @PathVariable Long assignmentId,
                                            @Valid @RequestBody AssignmentScoreRequest request) {
        return assignments.saveAssignmentScore(auth.getName(), courseId, assignmentId, request);
    }
}


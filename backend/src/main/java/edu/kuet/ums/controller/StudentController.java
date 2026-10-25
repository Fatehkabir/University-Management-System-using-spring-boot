package edu.kuet.ums.controller;

import edu.kuet.ums.dto.Requests.SelfEnrollRequest;
import edu.kuet.ums.dto.Responses.AttendanceResponse;
import edu.kuet.ums.dto.Responses.EnrollmentResponse;
import edu.kuet.ums.dto.Responses.StudentAssignmentItem;
import edu.kuet.ums.dto.Responses.StudentResponse;
import edu.kuet.ums.service.AssignmentService;
import edu.kuet.ums.service.AttendanceService;
import edu.kuet.ums.service.EnrollmentService;
import edu.kuet.ums.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService students;
    private final EnrollmentService enrollments;
    private final AttendanceService attendance;
    private final AssignmentService assignments;

    @GetMapping("/profile")
    public StudentResponse profile(Authentication auth) {
        return students.profile(auth.getName());
    }

    @GetMapping("/enrollments")
    public List<EnrollmentResponse> myEnrollments(Authentication auth) {
        return enrollments.forStudent(auth.getName());
    }

    @PostMapping("/enrollments")
    @ResponseStatus(HttpStatus.CREATED)
    public EnrollmentResponse enroll(Authentication auth, @Valid @RequestBody SelfEnrollRequest request) {
        return enrollments.selfEnroll(auth.getName(), request.courseId());
    }

    @DeleteMapping("/enrollments/{id}")
    public EnrollmentResponse drop(Authentication auth, @PathVariable Long id) {
        return enrollments.drop(auth.getName(), id);
    }

    @GetMapping("/attendance")
    public List<AttendanceResponse> myAttendance(Authentication auth) {
        return attendance.forStudent(auth.getName());
    }

    @GetMapping("/assignments")
    public List<StudentAssignmentItem> myAssignments(Authentication auth) {
        return assignments.getAssignmentsForStudent(auth.getName());
    }
}


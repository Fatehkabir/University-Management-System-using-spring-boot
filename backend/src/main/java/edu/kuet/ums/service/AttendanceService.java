package edu.kuet.ums.service;

import edu.kuet.ums.dto.Requests.AttendanceEntry;
import edu.kuet.ums.dto.Requests.AttendanceRequest;
import edu.kuet.ums.dto.Responses.AttendanceResponse;
import edu.kuet.ums.entity.Attendance;
import edu.kuet.ums.entity.Course;
import edu.kuet.ums.entity.Enrollment;
import edu.kuet.ums.entity.Teacher;
import edu.kuet.ums.entity.enums.EnrollmentStatus;
import edu.kuet.ums.exception.ApiExceptions.BadRequestException;
import edu.kuet.ums.exception.ApiExceptions.ResourceNotFoundException;
import edu.kuet.ums.mapper.EntityMapper;
import edu.kuet.ums.repository.AttendanceRepository;
import edu.kuet.ums.repository.EnrollmentRepository;
import edu.kuet.ums.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttendanceService {

    private final AttendanceRepository attendance;
    private final EnrollmentRepository enrollments;
    private final TeacherRepository teachers;
    private final EnrollmentService enrollmentService;
    private final EntityMapper mapper;

    @Transactional
    public List<AttendanceResponse> record(String teacherUsername, Long courseId, AttendanceRequest r) {
        Course course = enrollmentService.requireOwnCourse(teacherUsername, courseId);
        if (r.date().isAfter(LocalDate.now())) {
            throw new BadRequestException("Attendance cannot be recorded for a future date");
        }
        Teacher teacher = teachers.findByUsername(teacherUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher", teacherUsername));

        List<Attendance> saved = new ArrayList<>();
        for (AttendanceEntry entry : r.entries()) {
            Enrollment e = enrollments.findByStudentIdAndCourseId(entry.studentId(), courseId)
                    .filter(en -> en.getStatus() == EnrollmentStatus.ACTIVE)
                    .orElseThrow(() -> new BadRequestException(
                            "Student " + entry.studentId() + " is not actively enrolled in " + course.getCode()));
            Attendance a = attendance.findByStudentIdAndCourseIdAndDate(entry.studentId(), courseId, r.date())
                    .orElseGet(() -> new Attendance(e.getStudent(), course, r.date(), entry.status(), teacher));
            a.setStatus(entry.status());
            a.setRecordedBy(teacher);
            saved.add(attendance.save(a));
        }
        return saved.stream().map(mapper::toAttendance).toList();
    }

    public List<AttendanceResponse> forCourseOnDate(String teacherUsername, Long courseId, LocalDate date) {
        enrollmentService.requireOwnCourse(teacherUsername, courseId);
        return attendance.findByCourseIdAndDateOrderByStudentFullNameAsc(courseId, date).stream()
                .map(mapper::toAttendance).toList();
    }

    public List<AttendanceResponse> forStudent(String username) {
        return attendance.findByStudentUsernameOrderByDateDesc(username).stream()
                .map(mapper::toAttendance).toList();
    }
}


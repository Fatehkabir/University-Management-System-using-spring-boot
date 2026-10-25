package edu.kuet.ums.service;

import edu.kuet.ums.dto.Requests.GradeRequest;
import edu.kuet.ums.dto.Responses.EnrollmentResponse;
import edu.kuet.ums.entity.Course;
import edu.kuet.ums.entity.Enrollment;
import edu.kuet.ums.entity.Student;
import edu.kuet.ums.entity.enums.EnrollmentStatus;
import edu.kuet.ums.exception.ApiExceptions.ConflictException;
import edu.kuet.ums.exception.ApiExceptions.ResourceNotFoundException;
import edu.kuet.ums.mapper.EntityMapper;
import edu.kuet.ums.repository.AttendanceRepository;
import edu.kuet.ums.repository.CourseRepository;
import edu.kuet.ums.repository.EnrollmentRepository;
import edu.kuet.ums.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EnrollmentService {

    private final EnrollmentRepository enrollments;
    private final StudentRepository students;
    private final CourseRepository courses;
    private final AttendanceRepository attendance;
    private final EntityMapper mapper;

    public List<EnrollmentResponse> findAll() {
        return enrollments.findAllByOrderByEnrolledAtDesc().stream().map(mapper::toEnrollment).toList();
    }

    @Transactional
    public EnrollmentResponse enroll(Long studentId, Long courseId) {
        Student s = students.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", studentId));
        return enrollStudent(s, courseId);
    }

    @Transactional
    public void remove(Long enrollmentId) {
        Enrollment e = enrollments.findWithDetailsById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment", enrollmentId));
        attendance.findByStudentUsernameOrderByDateDesc(e.getStudent().getUsername()).stream()
                .filter(a -> a.getCourse().getId().equals(e.getCourse().getId()))
                .forEach(attendance::delete);
        enrollments.delete(e);
    }

    public List<EnrollmentResponse> forStudent(String username) {
        return enrollments.findByStudentUsernameOrderByEnrolledAtDesc(username).stream()
                .map(mapper::toEnrollment).toList();
    }

    @Transactional
    public EnrollmentResponse selfEnroll(String username, Long courseId) {
        Student s = students.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Student", username));
        return enrollStudent(s, courseId);
    }

    @Transactional
    public EnrollmentResponse drop(String username, Long enrollmentId) {
        Enrollment e = load(enrollmentId);
        if (!e.getStudent().getUsername().equals(username)) {
            throw new AccessDeniedException("Not your enrollment");
        }
        if (e.getStatus() == EnrollmentStatus.COMPLETED) {
            throw new ConflictException("A completed course cannot be dropped");
        }
        e.setStatus(EnrollmentStatus.DROPPED);
        return mapper.toEnrollment(e);
    }

    public List<EnrollmentResponse> roster(String teacherUsername, Long courseId) {
        requireOwnCourse(teacherUsername, courseId);
        return enrollments.findByCourseIdOrderByStudentFullNameAsc(courseId).stream()
                .map(mapper::toEnrollment).toList();
    }

    @Transactional
    public EnrollmentResponse grade(String teacherUsername, Long enrollmentId, GradeRequest r) {
        Enrollment e = load(enrollmentId);
        requireOwnCourse(teacherUsername, e.getCourse().getId());
        boolean hasGrade = r.grade() != null && !r.grade().isBlank();
        e.setGrade(hasGrade ? r.grade() : null);
        if (r.status() != null) {
            e.setStatus(r.status());
        } else if (hasGrade) {
            e.setStatus(EnrollmentStatus.COMPLETED);
        }
        return mapper.toEnrollment(e);
    }

    public Course requireOwnCourse(String teacherUsername, Long courseId) {
        Course c = courses.findById(courseId).orElseThrow(() -> new ResourceNotFoundException("Course", courseId));
        if (c.getTeacher() == null || !c.getTeacher().getUsername().equals(teacherUsername)) {
            throw new AccessDeniedException("You do not teach this course");
        }
        return c;
    }

    private EnrollmentResponse enrollStudent(Student s, Long courseId) {
        Course c = courses.findById(courseId).orElseThrow(() -> new ResourceNotFoundException("Course", courseId));
        Enrollment existing = enrollments.findByStudentIdAndCourseId(s.getId(), courseId).orElse(null);
        if (existing != null) {
            if (existing.getStatus() != EnrollmentStatus.DROPPED) {
                throw new ConflictException("Already enrolled in " + c.getCode());
            }
            existing.setStatus(EnrollmentStatus.ACTIVE);
            existing.setGrade(null);
            return mapper.toEnrollment(existing);
        }
        return mapper.toEnrollment(enrollments.save(new Enrollment(s, c)));
    }

    private Enrollment load(Long id) {
        return enrollments.findWithDetailsById(id).orElseThrow(() -> new ResourceNotFoundException("Enrollment", id));
    }
}


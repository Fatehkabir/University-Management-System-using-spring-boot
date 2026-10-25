package edu.kuet.ums.service;

import edu.kuet.ums.dto.Requests.AssignmentRequest;
import edu.kuet.ums.dto.Requests.AssignmentScoreRequest;
import edu.kuet.ums.dto.Responses.AssignmentResponse;
import edu.kuet.ums.dto.Responses.AssignmentScoreResponse;
import edu.kuet.ums.dto.Responses.StudentAssignmentItem;
import edu.kuet.ums.entity.*;
import edu.kuet.ums.entity.enums.EnrollmentStatus;
import edu.kuet.ums.exception.ApiExceptions.BadRequestException;
import edu.kuet.ums.exception.ApiExceptions.ForbiddenException;
import edu.kuet.ums.exception.ApiExceptions.ResourceNotFoundException;
import edu.kuet.ums.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final AssignmentScoreRepository scoreRepository;
    private final CourseRepository courseRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final EnrollmentRepository enrollmentRepository;

    private Course getTeacherCourse(String teacherUsername, Long courseId) {
        Teacher teacher = teacherRepository.findByUsername(teacherUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher", teacherUsername));
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));
        if (course.getTeacher() == null || !course.getTeacher().getId().equals(teacher.getId())) {
            throw new ForbiddenException("You do not teach this course");
        }
        return course;
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> getAssignmentsForTeacher(String teacherUsername, Long courseId) {
        getTeacherCourse(teacherUsername, courseId);
        List<Assignment> list = assignmentRepository.findByCourseIdOrderByDueDateAscCreatedAtDesc(courseId);
        return list.stream().map(a -> new AssignmentResponse(
                a.getId(),
                a.getCourse().getId(),
                a.getCourse().getCode(),
                a.getCourse().getTitle(),
                a.getTitle(),
                a.getDescription(),
                a.getMaxMarks(),
                a.getDueDate(),
                a.getCreatedAt(),
                a.getScores().size()
        )).toList();
    }

    @Transactional
    public AssignmentResponse createAssignment(String teacherUsername, Long courseId, AssignmentRequest request) {
        Course course = getTeacherCourse(teacherUsername, courseId);
        Assignment assignment = new Assignment(course, request.title(), request.description(), request.maxMarks(), request.dueDate());
        Assignment saved = assignmentRepository.save(assignment);
        return new AssignmentResponse(
                saved.getId(),
                course.getId(),
                course.getCode(),
                course.getTitle(),
                saved.getTitle(),
                saved.getDescription(),
                saved.getMaxMarks(),
                saved.getDueDate(),
                saved.getCreatedAt(),
                0
        );
    }

    @Transactional
    public void deleteAssignment(String teacherUsername, Long courseId, Long assignmentId) {
        getTeacherCourse(teacherUsername, courseId);
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment", assignmentId));
        if (!assignment.getCourse().getId().equals(courseId)) {
            throw new BadRequestException("Assignment does not belong to this course");
        }
        assignmentRepository.delete(assignment);
    }

    @Transactional(readOnly = true)
    public List<AssignmentScoreResponse> getAssignmentScores(String teacherUsername, Long courseId, Long assignmentId) {
        getTeacherCourse(teacherUsername, courseId);
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment", assignmentId));
        if (!assignment.getCourse().getId().equals(courseId)) {
            throw new BadRequestException("Assignment does not belong to this course");
        }

        List<Enrollment> enrollments = enrollmentRepository.findByCourseIdOrderByStudentFullNameAsc(courseId);
        List<AssignmentScore> scores = scoreRepository.findByAssignmentId(assignmentId);
        Map<Long, AssignmentScore> scoreMap = scores.stream()
                .collect(Collectors.toMap(s -> s.getStudent().getId(), s -> s, (s1, s2) -> s1));

        return enrollments.stream().map(e -> {
            Student s = e.getStudent();
            AssignmentScore score = scoreMap.get(s.getId());
            return new AssignmentScoreResponse(
                    score != null ? score.getId() : null,
                    assignment.getId(),
                    s.getId(),
                    s.getFullName(),
                    s.getStudentId(),
                    score != null ? score.getObtainedMarks() : null,
                    assignment.getMaxMarks(),
                    score != null ? score.getFeedback() : null,
                    score != null ? score.getGradedAt() : null
            );
        }).toList();
    }

    @Transactional
    public AssignmentScoreResponse saveAssignmentScore(String teacherUsername, Long courseId, Long assignmentId, AssignmentScoreRequest req) {
        getTeacherCourse(teacherUsername, courseId);
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment", assignmentId));
        if (!assignment.getCourse().getId().equals(courseId)) {
            throw new BadRequestException("Assignment does not belong to this course");
        }
        Student student = studentRepository.findById(req.studentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student", req.studentId()));

        if (req.obtainedMarks().compareTo(assignment.getMaxMarks()) > 0) {
            throw new BadRequestException("Obtained marks cannot exceed max marks (" + assignment.getMaxMarks() + ")");
        }

        AssignmentScore score = scoreRepository.findByAssignmentIdAndStudentId(assignmentId, student.getId())
                .orElseGet(() -> new AssignmentScore(assignment, student, req.obtainedMarks(), req.feedback()));
        score.setObtainedMarks(req.obtainedMarks());
        score.setFeedback(req.feedback());
        AssignmentScore saved = scoreRepository.save(score);

        return new AssignmentScoreResponse(
                saved.getId(),
                assignment.getId(),
                student.getId(),
                student.getFullName(),
                student.getStudentId(),
                saved.getObtainedMarks(),
                assignment.getMaxMarks(),
                saved.getFeedback(),
                saved.getGradedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<StudentAssignmentItem> getAssignmentsForStudent(String studentUsername) {
        Student student = studentRepository.findByUsername(studentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Student", studentUsername));
        List<Enrollment> enrollments = enrollmentRepository.findByStudentUsernameOrderByEnrolledAtDesc(studentUsername).stream()
                .filter(e -> e.getStatus() != EnrollmentStatus.DROPPED)
                .toList();
        if (enrollments.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> courseIds = enrollments.stream().map(e -> e.getCourse().getId()).toList();
        List<Assignment> assignments = assignmentRepository.findByCourseIdInOrderByDueDateAscCreatedAtDesc(courseIds);
        List<AssignmentScore> myScores = scoreRepository.findByStudentId(student.getId());
        Map<Long, AssignmentScore> scoreMap = myScores.stream()
                .collect(Collectors.toMap(s -> s.getAssignment().getId(), s -> s, (s1, s2) -> s1));

        return assignments.stream().map(a -> {
            AssignmentScore score = scoreMap.get(a.getId());
            return new StudentAssignmentItem(
                    a.getId(),
                    a.getCourse().getId(),
                    a.getCourse().getCode(),
                    a.getCourse().getTitle(),
                    a.getTitle(),
                    a.getDescription(),
                    a.getMaxMarks(),
                    a.getDueDate(),
                    score != null ? score.getObtainedMarks() : null,
                    score != null ? score.getFeedback() : null,
                    score != null ? score.getGradedAt() : null
            );
        }).toList();
    }
}


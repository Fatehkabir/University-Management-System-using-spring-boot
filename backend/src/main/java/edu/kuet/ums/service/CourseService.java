package edu.kuet.ums.service;

import edu.kuet.ums.dto.Requests.CourseRequest;
import edu.kuet.ums.dto.Responses.CourseResponse;
import edu.kuet.ums.entity.Course;
import edu.kuet.ums.entity.Teacher;
import edu.kuet.ums.exception.ApiExceptions.ConflictException;
import edu.kuet.ums.exception.ApiExceptions.ResourceNotFoundException;
import edu.kuet.ums.mapper.EntityMapper;
import edu.kuet.ums.repository.AttendanceRepository;
import edu.kuet.ums.repository.CourseRepository;
import edu.kuet.ums.repository.DepartmentRepository;
import edu.kuet.ums.repository.EnrollmentRepository;
import edu.kuet.ums.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {

    private final CourseRepository courses;
    private final DepartmentRepository departments;
    private final TeacherRepository teachers;
    private final EnrollmentRepository enrollments;
    private final AttendanceRepository attendance;
    private final EntityMapper mapper;

    public List<CourseResponse> findAll() {
        return courses.findAll().stream().map(mapper::toCourse).toList();
    }

    public CourseResponse findById(Long id) {
        return mapper.toCourse(get(id));
    }

    public List<CourseResponse> findByTeacher(String username) {
        return courses.findByTeacherUsername(username).stream().map(mapper::toCourse).toList();
    }

    @Transactional
    public CourseResponse create(CourseRequest r) {
        courses.findByCode(r.code().trim().toUpperCase()).ifPresent(c -> {
            throw new ConflictException("Course code already exists: " + c.getCode());
        });
        Course c = new Course();
        apply(c, r);
        return mapper.toCourse(courses.save(c));
    }

    @Transactional
    public CourseResponse update(Long id, CourseRequest r) {
        Course c = get(id);
        courses.findByCode(r.code().trim().toUpperCase()).filter(o -> !o.getId().equals(id)).ifPresent(o -> {
            throw new ConflictException("Course code already exists: " + o.getCode());
        });
        apply(c, r);
        return mapper.toCourse(c);
    }

    @Transactional
    public void delete(Long id) {
        get(id);
        attendance.deleteByCourseId(id);
        enrollments.deleteByCourseId(id);
        courses.deleteById(id);
    }

    private void apply(Course c, CourseRequest r) {
        c.setCode(r.code().trim().toUpperCase());
        c.setTitle(r.title().trim());
        c.setCreditHours(r.creditHours());
        c.setDepartment(departments.findById(r.departmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department", r.departmentId())));
        Teacher teacher = r.teacherId() == null ? null : teachers.findById(r.teacherId())
                .orElseThrow(() -> new ResourceNotFoundException("Teacher", r.teacherId()));
        c.setTeacher(teacher);
    }

    private Course get(Long id) {
        return courses.findById(id).orElseThrow(() -> new ResourceNotFoundException("Course", id));
    }
}


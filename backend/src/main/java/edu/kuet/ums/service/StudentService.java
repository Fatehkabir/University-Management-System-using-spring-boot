package edu.kuet.ums.service;

import edu.kuet.ums.dto.Requests.StudentRequest;
import edu.kuet.ums.dto.Responses.StudentResponse;
import edu.kuet.ums.entity.Department;
import edu.kuet.ums.entity.Student;
import edu.kuet.ums.exception.ApiExceptions.BadRequestException;
import edu.kuet.ums.exception.ApiExceptions.ConflictException;
import edu.kuet.ums.exception.ApiExceptions.ResourceNotFoundException;
import edu.kuet.ums.mapper.EntityMapper;
import edu.kuet.ums.repository.AttendanceRepository;
import edu.kuet.ums.repository.DepartmentRepository;
import edu.kuet.ums.repository.EnrollmentRepository;
import edu.kuet.ums.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentService {

    private final StudentRepository students;
    private final DepartmentRepository departments;
    private final EnrollmentRepository enrollments;
    private final AttendanceRepository attendance;
    private final UserUniquenessValidator uniqueness;
    private final PasswordEncoder encoder;
    private final EntityMapper mapper;

    public List<StudentResponse> findAll() {
        return students.findAll().stream().map(mapper::toStudent).toList();
    }

    public StudentResponse findById(Long id) {
        return mapper.toStudent(get(id));
    }

    public StudentResponse profile(String username) {
        return mapper.toStudent(students.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Student", username)));
    }

    @Transactional
    public StudentResponse create(StudentRequest r) {
        if (r.password() == null || r.password().isBlank()) {
            throw new BadRequestException("Password is required when creating a student");
        }
        uniqueness.check(r.username(), r.email(), null);
        assertStudentIdFree(r.studentId(), null);
        Student s = new Student(r.username().trim(), encoder.encode(r.password()), r.email().trim(),
                r.fullName().trim(), r.phone(), r.address(), r.studentId().trim(), department(r.departmentId()),
                r.batch(), r.semester() == null ? 1 : r.semester());
        s.setEnabled(r.enabled() == null || r.enabled());
        return mapper.toStudent(students.save(s));
    }

    @Transactional
    public StudentResponse update(Long id, StudentRequest r) {
        Student s = get(id);
        uniqueness.check(r.username(), r.email(), id);
        assertStudentIdFree(r.studentId(), id);
        s.setUsername(r.username().trim());
        s.setEmail(r.email().trim());
        s.setFullName(r.fullName().trim());
        s.setPhone(r.phone());
        s.setAddress(r.address());
        s.setStudentId(r.studentId().trim());
        s.setDepartment(department(r.departmentId()));
        s.setBatch(r.batch());
        if (r.semester() != null) {
            s.setSemester(r.semester());
        }
        if (r.enabled() != null) {
            s.setEnabled(r.enabled());
        }
        if (r.password() != null && !r.password().isBlank()) {
            s.setPassword(encoder.encode(r.password()));
        }
        return mapper.toStudent(s);
    }

    @Transactional
    public void delete(Long id) {
        get(id);
        attendance.deleteByStudentId(id);
        enrollments.deleteByStudentId(id);
        students.deleteById(id);
    }

    private Student get(Long id) {
        return students.findById(id).orElseThrow(() -> new ResourceNotFoundException("Student", id));
    }

    private Department department(Long id) {
        return departments.findById(id).orElseThrow(() -> new ResourceNotFoundException("Department", id));
    }

    private void assertStudentIdFree(String studentId, Long selfId) {
        students.findByStudentId(studentId.trim()).filter(s -> !s.getId().equals(selfId))
                .ifPresent(s -> {
                    throw new ConflictException("Student ID already in use: " + studentId);
                });
    }
}


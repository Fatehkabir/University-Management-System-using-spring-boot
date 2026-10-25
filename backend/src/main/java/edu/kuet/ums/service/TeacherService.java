package edu.kuet.ums.service;

import edu.kuet.ums.dto.Requests.TeacherRequest;
import edu.kuet.ums.dto.Responses.TeacherResponse;
import edu.kuet.ums.entity.Department;
import edu.kuet.ums.entity.Teacher;
import edu.kuet.ums.exception.ApiExceptions.BadRequestException;
import edu.kuet.ums.exception.ApiExceptions.ConflictException;
import edu.kuet.ums.exception.ApiExceptions.ResourceNotFoundException;
import edu.kuet.ums.mapper.EntityMapper;
import edu.kuet.ums.repository.AttendanceRepository;
import edu.kuet.ums.repository.CourseRepository;
import edu.kuet.ums.repository.DepartmentRepository;
import edu.kuet.ums.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeacherService {

    private final TeacherRepository teachers;
    private final DepartmentRepository departments;
    private final CourseRepository courses;
    private final AttendanceRepository attendance;
    private final UserUniquenessValidator uniqueness;
    private final PasswordEncoder encoder;
    private final EntityMapper mapper;

    public List<TeacherResponse> findAll() {
        return teachers.findAll().stream().map(mapper::toTeacher).toList();
    }

    public TeacherResponse findById(Long id) {
        return mapper.toTeacher(get(id));
    }

    public TeacherResponse profile(String username) {
        return mapper.toTeacher(teachers.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher", username)));
    }

    @Transactional
    public TeacherResponse create(TeacherRequest r) {
        if (r.password() == null || r.password().isBlank()) {
            throw new BadRequestException("Password is required when creating a teacher");
        }
        uniqueness.check(r.username(), r.email(), null);
        assertEmployeeIdFree(r.employeeId(), null);
        Teacher t = new Teacher(r.username().trim(), encoder.encode(r.password()), r.email().trim(),
                r.fullName().trim(), r.phone(), r.address(), r.employeeId().trim(), department(r.departmentId()),
                r.designation(), r.qualification(), r.joiningDate());
        t.setEnabled(r.enabled() == null || r.enabled());
        return mapper.toTeacher(teachers.save(t));
    }

    @Transactional
    public TeacherResponse update(Long id, TeacherRequest r) {
        Teacher t = get(id);
        uniqueness.check(r.username(), r.email(), id);
        assertEmployeeIdFree(r.employeeId(), id);
        t.setUsername(r.username().trim());
        t.setEmail(r.email().trim());
        t.setFullName(r.fullName().trim());
        t.setPhone(r.phone());
        t.setAddress(r.address());
        t.setEmployeeId(r.employeeId().trim());
        t.setDepartment(department(r.departmentId()));
        t.setDesignation(r.designation());
        t.setQualification(r.qualification());
        t.setJoiningDate(r.joiningDate());
        if (r.enabled() != null) {
            t.setEnabled(r.enabled());
        }
        if (r.password() != null && !r.password().isBlank()) {
            t.setPassword(encoder.encode(r.password()));
        }
        return mapper.toTeacher(t);
    }

    @Transactional
    public void delete(Long id) {
        get(id);
        courses.unassignTeacher(id);
        attendance.clearRecorder(id);
        teachers.deleteById(id);
    }

    private Teacher get(Long id) {
        return teachers.findById(id).orElseThrow(() -> new ResourceNotFoundException("Teacher", id));
    }

    private Department department(Long id) {
        return departments.findById(id).orElseThrow(() -> new ResourceNotFoundException("Department", id));
    }

    private void assertEmployeeIdFree(String employeeId, Long selfId) {
        teachers.findByEmployeeId(employeeId.trim()).filter(t -> !t.getId().equals(selfId))
                .ifPresent(t -> {
                    throw new ConflictException("Employee ID already in use: " + employeeId);
                });
    }
}


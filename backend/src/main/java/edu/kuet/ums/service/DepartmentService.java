package edu.kuet.ums.service;

import edu.kuet.ums.dto.Requests.DepartmentRequest;
import edu.kuet.ums.dto.Responses.DepartmentResponse;
import edu.kuet.ums.entity.Department;
import edu.kuet.ums.exception.ApiExceptions.ConflictException;
import edu.kuet.ums.exception.ApiExceptions.ResourceNotFoundException;
import edu.kuet.ums.mapper.EntityMapper;
import edu.kuet.ums.repository.CourseRepository;
import edu.kuet.ums.repository.DepartmentRepository;
import edu.kuet.ums.repository.StudentRepository;
import edu.kuet.ums.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DepartmentService {

    private final DepartmentRepository departments;
    private final TeacherRepository teachers;
    private final StudentRepository students;
    private final CourseRepository courses;
    private final EntityMapper mapper;

    public List<DepartmentResponse> findAll() {
        return departments.findAllByOrderByNameAsc().stream().map(mapper::toDepartment).toList();
    }

    public DepartmentResponse findById(Long id) {
        return mapper.toDepartment(get(id));
    }

    @Transactional
    public DepartmentResponse create(DepartmentRequest r) {
        assertUnique(r, null);
        return mapper.toDepartment(departments.save(
                Department.builder().name(r.name().trim()).code(r.code().trim().toUpperCase()).build()));
    }

    @Transactional
    public DepartmentResponse update(Long id, DepartmentRequest r) {
        Department d = get(id);
        assertUnique(r, id);
        d.setName(r.name().trim());
        d.setCode(r.code().trim().toUpperCase());
        return mapper.toDepartment(d);
    }

    @Transactional
    public void delete(Long id) {
        get(id);
        if (teachers.existsByDepartmentId(id) || students.existsByDepartmentId(id)
                || courses.existsByDepartmentId(id)) {
            throw new ConflictException("Department still has teachers, students or courses");
        }
        departments.deleteById(id);
    }

    Department get(Long id) {
        return departments.findById(id).orElseThrow(() -> new ResourceNotFoundException("Department", id));
    }

    private void assertUnique(DepartmentRequest r, Long selfId) {
        departments.findByCode(r.code().trim().toUpperCase()).filter(d -> !d.getId().equals(selfId))
                .ifPresent(d -> {
                    throw new ConflictException("Department code already exists: " + d.getCode());
                });
        departments.findByName(r.name().trim()).filter(d -> !d.getId().equals(selfId))
                .ifPresent(d -> {
                    throw new ConflictException("Department name already exists: " + d.getName());
                });
    }
}


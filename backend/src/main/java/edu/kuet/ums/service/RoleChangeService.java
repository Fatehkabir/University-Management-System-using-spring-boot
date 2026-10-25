package edu.kuet.ums.service;

import edu.kuet.ums.dto.Requests.ConvertToStudentRequest;
import edu.kuet.ums.dto.Requests.ConvertToTeacherRequest;
import edu.kuet.ums.dto.Responses.StudentResponse;
import edu.kuet.ums.dto.Responses.TeacherResponse;
import edu.kuet.ums.entity.Student;
import edu.kuet.ums.entity.Teacher;
import edu.kuet.ums.exception.ApiExceptions.ConflictException;
import edu.kuet.ums.exception.ApiExceptions.ResourceNotFoundException;
import edu.kuet.ums.mapper.EntityMapper;
import edu.kuet.ums.repository.AttendanceRepository;
import edu.kuet.ums.repository.DepartmentRepository;
import edu.kuet.ums.repository.EnrollmentRepository;
import edu.kuet.ums.repository.StudentRepository;
import edu.kuet.ums.repository.TeacherRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoleChangeService {

    private final JdbcTemplate jdbc;
    private final EntityManager em;
    private final TeacherRepository teachers;
    private final StudentRepository students;
    private final DepartmentRepository departments;
    private final EnrollmentRepository enrollments;
    private final AttendanceRepository attendance;
    private final EntityMapper mapper;

    @Transactional
    public StudentResponse teacherToStudent(Long id, ConvertToStudentRequest r) {
        Teacher teacher = teachers.findById(id).orElseThrow(() -> new ResourceNotFoundException("Teacher", id));
        String studentCode = r.studentId().trim();
        students.findByStudentId(studentCode).ifPresent(s -> {
            throw new ConflictException("Student ID already in use: " + studentCode);
        });
        Long departmentId = resolveDepartment(r.departmentId(), teacher.getDepartment().getId());
        int semester = r.semester() == null ? 1 : r.semester();

        jdbc.update("update courses set teacher_id = null where teacher_id = ?", id);
        jdbc.update("update attendance set recorded_by = null where recorded_by = ?", id);
        jdbc.update("delete from teachers where id = ?", id);
        jdbc.update("update users set role = 'STUDENT', updated_at = current_timestamp where id = ?", id);
        jdbc.update("insert into students (id, student_id, department_id, batch, semester) values (?, ?, ?, ?, ?)",
                id, studentCode, departmentId, blankToNull(r.batch()), semester);

        em.clear();
        return mapper.toStudent(students.findById(id).orElseThrow(() -> new ResourceNotFoundException("Student", id)));
    }

    @Transactional
    public TeacherResponse studentToTeacher(Long id, ConvertToTeacherRequest r) {
        Student student = students.findById(id).orElseThrow(() -> new ResourceNotFoundException("Student", id));
        String employeeCode = r.employeeId().trim();
        teachers.findByEmployeeId(employeeCode).ifPresent(t -> {
            throw new ConflictException("Employee ID already in use: " + employeeCode);
        });

        long enrolled = enrollments.countByStudentId(id);
        long classes = attendance.countByStudentId(id);
        if ((enrolled > 0 || classes > 0) && !Boolean.TRUE.equals(r.deleteRecords())) {
            throw new ConflictException("This student has " + enrolled + " enrollment(s) and " + classes
                    + " attendance record(s). Changing the role deletes them, so please confirm.");
        }
        Long departmentId = resolveDepartment(r.departmentId(), student.getDepartment().getId());

        jdbc.update("delete from attendance where student_id = ?", id);
        jdbc.update("delete from enrollments where student_id = ?", id);
        jdbc.update("delete from students where id = ?", id);
        jdbc.update("update users set role = 'TEACHER', updated_at = current_timestamp where id = ?", id);
        jdbc.update("insert into teachers (id, employee_id, department_id, designation, qualification, joining_date) "
                        + "values (?, ?, ?, ?, ?, ?)",
                id, employeeCode, departmentId, blankToNull(r.designation()), blankToNull(r.qualification()),
                r.joiningDate());

        em.clear();
        return mapper.toTeacher(teachers.findById(id).orElseThrow(() -> new ResourceNotFoundException("Teacher", id)));
    }

    private Long resolveDepartment(Long requested, Long current) {
        if (requested == null) {
            return current;
        }
        return departments.findById(requested)
                .orElseThrow(() -> new ResourceNotFoundException("Department", requested)).getId();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}


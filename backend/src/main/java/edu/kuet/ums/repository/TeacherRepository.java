package edu.kuet.ums.repository;

import edu.kuet.ums.entity.Teacher;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    @Override
    @EntityGraph(attributePaths = "department")
    List<Teacher> findAll();

    @Override
    @EntityGraph(attributePaths = "department")
    Optional<Teacher> findById(Long id);

    @EntityGraph(attributePaths = "department")
    Optional<Teacher> findByUsername(String username);

    Optional<Teacher> findByEmployeeId(String employeeId);

    boolean existsByDepartmentId(Long departmentId);
}


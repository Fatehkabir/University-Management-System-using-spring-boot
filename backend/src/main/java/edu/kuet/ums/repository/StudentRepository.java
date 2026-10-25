package edu.kuet.ums.repository;

import edu.kuet.ums.entity.Student;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    @Override
    @EntityGraph(attributePaths = "department")
    List<Student> findAll();

    @Override
    @EntityGraph(attributePaths = "department")
    Optional<Student> findById(Long id);

    @EntityGraph(attributePaths = "department")
    Optional<Student> findByUsername(String username);

    Optional<Student> findByStudentId(String studentId);

    boolean existsByDepartmentId(Long departmentId);

    @Query("select d.name, count(s) from Student s join s.department d group by d.id, d.name "
            + "order by count(s) desc, d.name asc")
    List<Object[]> countByDepartment();
}


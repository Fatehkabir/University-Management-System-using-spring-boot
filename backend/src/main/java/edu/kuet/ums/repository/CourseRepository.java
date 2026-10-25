package edu.kuet.ums.repository;

import edu.kuet.ums.entity.Course;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    @Override
    @EntityGraph(attributePaths = {"department", "teacher"})
    List<Course> findAll();

    @Override
    @EntityGraph(attributePaths = {"department", "teacher"})
    Optional<Course> findById(Long id);

    @EntityGraph(attributePaths = {"department", "teacher"})
    List<Course> findByTeacherUsername(String username);

    Optional<Course> findByCode(String code);

    boolean existsByDepartmentId(Long departmentId);

    long countByTeacherIsNull();

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Course c set c.teacher = null where c.teacher.id = :teacherId")
    void unassignTeacher(@Param("teacherId") Long teacherId);
}


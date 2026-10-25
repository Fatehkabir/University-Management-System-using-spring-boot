package edu.kuet.ums.repository;

import edu.kuet.ums.entity.Enrollment;
import edu.kuet.ums.entity.enums.EnrollmentStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    @EntityGraph(attributePaths = {"student", "course", "course.teacher"})
    List<Enrollment> findAllByOrderByEnrolledAtDesc();

    @EntityGraph(attributePaths = {"student", "course", "course.teacher"})
    List<Enrollment> findByStudentUsernameOrderByEnrolledAtDesc(String username);

    @EntityGraph(attributePaths = {"student", "course", "course.teacher"})
    List<Enrollment> findByCourseIdOrderByStudentFullNameAsc(Long courseId);

    @EntityGraph(attributePaths = {"student", "course", "course.teacher"})
    Optional<Enrollment> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {"student", "course", "course.teacher"})
    Optional<Enrollment> findByStudentIdAndCourseId(Long studentId, Long courseId);

    void deleteByStudentId(Long studentId);

    void deleteByCourseId(Long courseId);

    long countByStatus(EnrollmentStatus status);

    long countByStudentId(Long studentId);

    long countByCourseIdAndStatusNot(Long courseId, EnrollmentStatus status);

    long countByCourseIdAndGradeIsNotNull(Long courseId);

    @Query("select c.code, c.title, count(e) from Enrollment e join e.course c "
            + "where e.status <> :excluded group by c.id, c.code, c.title order by count(e) desc, c.code asc")
    List<Object[]> topCourses(@Param("excluded") EnrollmentStatus excluded, Pageable pageable);
}


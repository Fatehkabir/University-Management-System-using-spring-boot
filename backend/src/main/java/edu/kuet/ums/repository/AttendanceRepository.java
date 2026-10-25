package edu.kuet.ums.repository;

import edu.kuet.ums.entity.Attendance;
import edu.kuet.ums.entity.enums.AttendanceStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    @EntityGraph(attributePaths = {"student", "course"})
    List<Attendance> findByCourseIdAndDateOrderByStudentFullNameAsc(Long courseId, LocalDate date);

    @EntityGraph(attributePaths = {"student", "course"})
    List<Attendance> findByStudentUsernameOrderByDateDesc(String username);

    Optional<Attendance> findByStudentIdAndCourseIdAndDate(Long studentId, Long courseId, LocalDate date);

    void deleteByStudentId(Long studentId);

    void deleteByCourseId(Long courseId);

    long countByStudentId(Long studentId);

    long countByCourseId(Long courseId);

    long countByStatusIn(Collection<AttendanceStatus> statuses);

    long countByCourseIdAndStatusIn(Long courseId, Collection<AttendanceStatus> statuses);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Attendance a set a.recordedBy = null where a.recordedBy.id = :teacherId")
    void clearRecorder(@Param("teacherId") Long teacherId);
}


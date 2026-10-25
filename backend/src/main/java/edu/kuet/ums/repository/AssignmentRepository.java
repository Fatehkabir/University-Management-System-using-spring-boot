package edu.kuet.ums.repository;

import edu.kuet.ums.entity.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    List<Assignment> findByCourseIdOrderByDueDateAscCreatedAtDesc(Long courseId);
    List<Assignment> findByCourseIdInOrderByDueDateAscCreatedAtDesc(List<Long> courseIds);
}


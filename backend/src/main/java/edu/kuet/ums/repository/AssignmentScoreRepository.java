package edu.kuet.ums.repository;

import edu.kuet.ums.entity.AssignmentScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssignmentScoreRepository extends JpaRepository<AssignmentScore, Long> {
    List<AssignmentScore> findByAssignmentId(Long assignmentId);
    List<AssignmentScore> findByStudentId(Long studentId);
    Optional<AssignmentScore> findByAssignmentIdAndStudentId(Long assignmentId, Long studentId);
}


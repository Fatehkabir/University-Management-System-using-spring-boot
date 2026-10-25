package edu.kuet.ums.repository;

import edu.kuet.ums.entity.Announcement;
import edu.kuet.ums.entity.enums.Role;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    @EntityGraph(attributePaths = "postedBy")
    List<Announcement> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = "postedBy")
    @Query("select a from Announcement a where a.targetRole is null or a.targetRole = :role order by a.createdAt desc")
    List<Announcement> findVisibleTo(@Param("role") Role role);
}


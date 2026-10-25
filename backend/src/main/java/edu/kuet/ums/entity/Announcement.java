package edu.kuet.ums.entity;

import edu.kuet.ums.entity.enums.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "announcements")
@Getter
@Setter
@NoArgsConstructor
public class Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "posted_by", nullable = false)
    private Admin postedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_role")
    private Role targetRole;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public Announcement(String title, String content, Admin postedBy, Role targetRole) {
        this.title = title;
        this.content = content;
        this.postedBy = postedBy;
        this.targetRole = targetRole;
    }
}


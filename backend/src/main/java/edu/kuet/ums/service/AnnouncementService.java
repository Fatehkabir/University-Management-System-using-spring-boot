package edu.kuet.ums.service;

import edu.kuet.ums.dto.Requests.AnnouncementRequest;
import edu.kuet.ums.dto.Responses.AnnouncementResponse;
import edu.kuet.ums.entity.Admin;
import edu.kuet.ums.entity.Announcement;
import edu.kuet.ums.entity.enums.Role;
import edu.kuet.ums.exception.ApiExceptions.ResourceNotFoundException;
import edu.kuet.ums.mapper.EntityMapper;
import edu.kuet.ums.repository.AdminRepository;
import edu.kuet.ums.repository.AnnouncementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnnouncementService {

    private final AnnouncementRepository announcements;
    private final AdminRepository admins;
    private final EntityMapper mapper;

    public List<AnnouncementResponse> visibleTo(Role role) {
        List<Announcement> list = role == Role.ADMIN
                ? announcements.findAllByOrderByCreatedAtDesc()
                : announcements.findVisibleTo(role);
        return list.stream().map(mapper::toAnnouncement).toList();
    }

    @Transactional
    public AnnouncementResponse create(String adminUsername, AnnouncementRequest r) {
        Admin admin = admins.findByUsername(adminUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Admin", adminUsername));
        return mapper.toAnnouncement(announcements.save(
                new Announcement(r.title().trim(), r.content().trim(), admin, normalise(r.targetRole()))));
    }

    @Transactional
    public AnnouncementResponse update(Long id, AnnouncementRequest r) {
        Announcement a = get(id);
        a.setTitle(r.title().trim());
        a.setContent(r.content().trim());
        a.setTargetRole(normalise(r.targetRole()));
        return mapper.toAnnouncement(a);
    }

    @Transactional
    public void delete(Long id) {
        get(id);
        announcements.deleteById(id);
    }

    private Role normalise(Role target) {
        return target == Role.ADMIN ? null : target;
    }

    private Announcement get(Long id) {
        return announcements.findById(id).orElseThrow(() -> new ResourceNotFoundException("Announcement", id));
    }
}


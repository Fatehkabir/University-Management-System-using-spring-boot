package edu.kuet.ums.controller;

import edu.kuet.ums.dto.Requests.AnnouncementRequest;
import edu.kuet.ums.dto.Responses.AnnouncementResponse;
import edu.kuet.ums.entity.enums.Role;
import edu.kuet.ums.service.AnnouncementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/announcements")
@RequiredArgsConstructor
public class AdminAnnouncementController {

    private final AnnouncementService service;

    @GetMapping
    public List<AnnouncementResponse> list() {
        return service.visibleTo(Role.ADMIN);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AnnouncementResponse create(Authentication auth, @Valid @RequestBody AnnouncementRequest request) {
        return service.create(auth.getName(), request);
    }

    @PutMapping("/{id}")
    public AnnouncementResponse update(@PathVariable Long id, @Valid @RequestBody AnnouncementRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}


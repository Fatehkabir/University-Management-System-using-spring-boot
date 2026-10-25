package edu.kuet.ums.controller;

import edu.kuet.ums.dto.Responses.AnnouncementResponse;
import edu.kuet.ums.dto.Responses.CourseResponse;
import edu.kuet.ums.dto.Responses.DepartmentResponse;
import edu.kuet.ums.security.AppUserDetails;
import edu.kuet.ums.service.AnnouncementService;
import edu.kuet.ums.service.CourseService;
import edu.kuet.ums.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CatalogController {

    private final DepartmentService departments;
    private final CourseService courses;
    private final AnnouncementService announcements;

    @GetMapping("/departments")
    public List<DepartmentResponse> departments() {
        return departments.findAll();
    }

    @GetMapping("/courses")
    public List<CourseResponse> courses() {
        return courses.findAll();
    }

    @GetMapping("/announcements")
    public List<AnnouncementResponse> announcements(Authentication auth) {
        return announcements.visibleTo(((AppUserDetails) auth.getPrincipal()).getRole());
    }
}


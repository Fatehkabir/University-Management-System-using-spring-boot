package edu.kuet.ums.controller;

import edu.kuet.ums.dto.Requests.ConvertToStudentRequest;
import edu.kuet.ums.dto.Requests.TeacherRequest;
import edu.kuet.ums.dto.Responses.StudentResponse;
import edu.kuet.ums.dto.Responses.TeacherResponse;
import edu.kuet.ums.service.RoleChangeService;
import edu.kuet.ums.service.TeacherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/teachers")
@RequiredArgsConstructor
public class AdminTeacherController {

    private final TeacherService service;
    private final RoleChangeService roleChange;

    @GetMapping
    public List<TeacherResponse> list() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public TeacherResponse get(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TeacherResponse create(@Valid @RequestBody TeacherRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public TeacherResponse update(@PathVariable Long id, @Valid @RequestBody TeacherRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @PostMapping("/{id}/convert-to-student")
    public StudentResponse convertToStudent(@PathVariable Long id, @Valid @RequestBody ConvertToStudentRequest request) {
        return roleChange.teacherToStudent(id, request);
    }
}


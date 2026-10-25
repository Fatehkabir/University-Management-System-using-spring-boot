package edu.kuet.ums.controller;

import edu.kuet.ums.dto.Requests.ConvertToTeacherRequest;
import edu.kuet.ums.dto.Requests.StudentRequest;
import edu.kuet.ums.dto.Responses.StudentResponse;
import edu.kuet.ums.dto.Responses.TeacherResponse;
import edu.kuet.ums.service.RoleChangeService;
import edu.kuet.ums.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/students")
@RequiredArgsConstructor
public class AdminStudentController {

    private final StudentService service;
    private final RoleChangeService roleChange;

    @GetMapping
    public List<StudentResponse> list() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public StudentResponse get(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StudentResponse create(@Valid @RequestBody StudentRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public StudentResponse update(@PathVariable Long id, @Valid @RequestBody StudentRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @PostMapping("/{id}/convert-to-teacher")
    public TeacherResponse convertToTeacher(@PathVariable Long id, @Valid @RequestBody ConvertToTeacherRequest request) {
        return roleChange.studentToTeacher(id, request);
    }
}


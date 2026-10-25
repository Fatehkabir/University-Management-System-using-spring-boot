package edu.kuet.ums.controller;

import edu.kuet.ums.dto.Responses.SignupInfo;
import edu.kuet.ums.service.SignupService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {

    private final SignupService signup;

    @GetMapping("/signup-info")
    public SignupInfo signupInfo() {
        return signup.info();
    }
}


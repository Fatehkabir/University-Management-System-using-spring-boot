package edu.kuet.ums.service;

import edu.kuet.ums.dto.Requests.ChangePasswordRequest;
import edu.kuet.ums.dto.Requests.LoginRequest;
import edu.kuet.ums.dto.Responses.AuthResponse;
import edu.kuet.ums.dto.Responses.UserSummary;
import edu.kuet.ums.entity.User;
import edu.kuet.ums.exception.ApiExceptions.BadRequestException;
import edu.kuet.ums.exception.ApiExceptions.ResourceNotFoundException;
import edu.kuet.ums.repository.UserRepository;
import edu.kuet.ums.security.AppUserDetails;
import edu.kuet.ums.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository users;
    private final PasswordEncoder encoder;

    public AuthResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        AppUserDetails user = (AppUserDetails) auth.getPrincipal();
        return new AuthResponse(jwtService.generateToken(user), "Bearer", jwtService.getExpirationMs(),
                new UserSummary(user.getId(), user.getUsername(), user.getFullName(), user.getRole()));
    }

    @Transactional(readOnly = true)
    public UserSummary me(String username) {
        User u = find(username);
        return new UserSummary(u.getId(), u.getUsername(), u.getFullName(), u.getRole());
    }

    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        User u = find(username);
        if (!encoder.matches(request.currentPassword(), u.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }
        u.setPassword(encoder.encode(request.newPassword()));
    }

    private User find(String username) {
        return users.findByUsername(username).orElseThrow(() -> new ResourceNotFoundException("User", username));
    }
}


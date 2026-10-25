package edu.kuet.ums.service;

import edu.kuet.ums.exception.ApiExceptions.ConflictException;
import edu.kuet.ums.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserUniquenessValidator {

    private final UserRepository users;

    public void check(String username, String email, Long selfId) {
        users.findByUsername(username)
                .filter(u -> !u.getId().equals(selfId))
                .ifPresent(u -> {
                    throw new ConflictException("Username already taken: " + username);
                });
        users.findByEmail(email)
                .filter(u -> !u.getId().equals(selfId))
                .ifPresent(u -> {
                    throw new ConflictException("Email already in use: " + email);
                });
    }
}


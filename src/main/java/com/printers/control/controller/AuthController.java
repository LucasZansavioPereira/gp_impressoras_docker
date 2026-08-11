package com.printers.control.controller;

import com.printers.control.model.User;
import com.printers.control.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public UserResponse login(@Valid @RequestBody LoginRequest request) {
        User user = userService.authenticate(request.username(), request.password());
        return new UserResponse(user.getId(), user.getUsername(), user.isDefaultUser());
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@Valid @RequestBody CreateUserRequest request) {
        User user = userService.createUser(request.username(), request.password());
        return new UserResponse(user.getId(), user.getUsername(), user.isDefaultUser());
    }

    @PutMapping("/users/{username}")
    public UserResponse updateUser(@PathVariable String username, @Valid @RequestBody UpdateUserRequest request) {
        User user = userService.updateUser(username, request.currentPassword(), request.newUsername(), request.newPassword());
        return new UserResponse(user.getId(), user.getUsername(), user.isDefaultUser());
    }

    @GetMapping("/users")
    public List<UserResponse> listUsers() {
        return userService.findAllUsers().stream()
                .map(user -> new UserResponse(user.getId(), user.getUsername(), user.isDefaultUser()))
                .toList();
    }

    @DeleteMapping("/users/{username}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable String username) {
        userService.deleteUser(username);
    }

    @PostMapping("/users/{username}/reset-password")
    public UserResponse resetPassword(@PathVariable String username, @Valid @RequestBody ResetPasswordRequest request) {
        User user = userService.resetPassword(username, request.password());
        return new UserResponse(user.getId(), user.getUsername(), user.isDefaultUser());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("erro", ex.getMessage()));
    }

    public record UpdateUserRequest(@NotBlank String currentPassword, String newUsername, String newPassword) {}

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
    public record CreateUserRequest(@NotBlank String username, @NotBlank String password) {}
    public record ResetPasswordRequest(@NotBlank String password) {}
    public record UserResponse(String id, String username, boolean isDefaultUser) {}
}

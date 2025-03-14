package com.messenger.auth_service.controllers;


import com.messenger.auth_service.dto.AuthDTO;
import com.messenger.auth_service.dto.RegistrationDTO;
import com.messenger.auth_service.dto.TokenInfo;
import com.messenger.auth_service.models.UserProfile;
import com.messenger.auth_service.repositories.UserProfileRepository;
import com.messenger.auth_service.security.JWTUtil;
import com.messenger.auth_service.services.AuthService;
import com.messenger.auth_service.services.RegistrationService;
import com.messenger.auth_service.utils.UserProfileValidator;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;

@RestController()
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000")
public class AuthController {

    private final RegistrationService registrationService;
    private final AuthService authService;
    private final JWTUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserProfileValidator userProfileValidator;
    private final UserProfileRepository userProfileRepository;
    private final ModelMapper modelMapper;

    @PostMapping("/registration")
    public ResponseEntity<?> performRegistration (@RequestBody RegistrationDTO registrationDTO,
                                                  BindingResult bindingResult) {
        UserProfile userProfile = modelMapper.map(registrationDTO, UserProfile.class);
        userProfileValidator.validate(userProfile, bindingResult);
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(bindingResult.getAllErrors());
        }
        registrationService.register(userProfile);
        jwtUtil.generateToken(userProfile.getUsername(), userProfile.getId());
        return ResponseEntity.ok(HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<?> performLogin(
            @RequestBody AuthDTO authDTO) {
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(authDTO.getUsername(), authDTO.getPassword());
        try {
            authenticationManager.authenticate(authToken);
        } catch (BadCredentialsException e) {
            return ResponseEntity.badRequest().build();
        }
        int id = userProfileRepository.findByUsername(authDTO.getUsername()).get().getId();
        String token = jwtUtil.generateToken(authDTO.getUsername(), id);
        return ResponseEntity.ok(Map.of("token", token));
    }

    @PostMapping("/verify")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String token) {
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token is missing");
        }

        String jwtToken = token.startsWith("Bearer ") ? token.substring(7) : token;

        try {
            TokenInfo tokenInfo = jwtUtil.extractTokenInfo(jwtToken);
            return ResponseEntity.ok(tokenInfo);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String token) {
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token is missing");
        }

        String jwtToken = token.startsWith("Bearer ") ? token.substring(7) : token;

        try {
            jwtUtil.verifyToken(jwtToken);
            try {
                authService.logout(jwtToken);
                return ResponseEntity.ok().build();
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Error during logout: " + e.getMessage());
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
        }
    }
}

//@PostMapping("/verify")
//public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String token) {
//    if (token == null || token.isEmpty()) {
//        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token is missing");
//    }
//
//    String jwtToken = token.startsWith("Bearer ") ? token.substring(7) : token;
//
//    try {
//        int userId = jwtUtil.extractUserId(jwtToken);
//        return ResponseEntity.ok(userId);
//    } catch (IllegalArgumentException e) {
//        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
//    }
//}
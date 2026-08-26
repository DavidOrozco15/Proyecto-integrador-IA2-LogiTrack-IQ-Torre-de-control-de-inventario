package com.logitrack.controller;

import com.logitrack.dto.AuthResponse;
import com.logitrack.dto.LoginRequest;
import com.logitrack.dto.RegisterEmpleadoRequest;
import com.logitrack.dto.RegisterRequest;
import com.logitrack.exception.BadRequestException;
import com.logitrack.model.Rol;
import com.logitrack.model.Usuario;
import com.logitrack.repository.UsuarioRepository;
import com.logitrack.security.JwtTokenProvider;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthController(AuthenticationManager authenticationManager,
                          UsuarioRepository usuarioRepository,
                          PasswordEncoder passwordEncoder,
                          JwtTokenProvider tokenProvider) {
        this.authenticationManager = authenticationManager;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        String token = tokenProvider.generateToken(authentication);

        try {
            Usuario usuario = usuarioRepository.findByUsername(loginRequest.getUsername())
                    .or(() -> usuarioRepository.findByEmail(loginRequest.getUsername()))
                    .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado después de autenticación"));

            return ResponseEntity.ok(AuthResponse.builder()
                    .token(token)
                    .tokenType("Bearer")
                    .userId(usuario.getId())
                    .username(usuario.getUsername())
                    .email(usuario.getEmail())
                    .rol(usuario.getRol())
                    .build());
        } catch (NoSuchElementException e) {
            throw new BadRequestException("Error al recuperar datos del usuario autenticado. Contacte al administrador.");
        }
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
        if (usuarioRepository.existsByUsername(registerRequest.getUsername())) {
            throw new BadRequestException("El nombre de usuario ya está en uso.");
        }

        if (usuarioRepository.existsByEmail(registerRequest.getEmail())) {
            throw new BadRequestException("El correo electrónico ya está registrado.");
        }

        // FORZAR rol = EMPLEADO siempre en registro público
        Rol rol = Rol.EMPLEADO;

        Usuario usuario = Usuario.builder()
                .username(registerRequest.getUsername().trim())
                .email(registerRequest.getEmail().trim().toLowerCase())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .rol(rol)
                .build();

        try {
            usuarioRepository.save(usuario);
        } catch (Exception e) {
            throw new BadRequestException("Error al registrar el usuario: " + e.getMessage());
        }

        return login(new LoginRequest(registerRequest.getUsername(), registerRequest.getPassword()));
    }

    @PostMapping("/register-empleado")
    public ResponseEntity<Map<String, Object>> registerEmpleado(@Valid @RequestBody RegisterEmpleadoRequest request) {
        if (usuarioRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("El nombre de usuario ya está en uso.");
        }

        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("El correo electrónico ya está registrado.");
        }

        Usuario usuario = Usuario.builder()
                .username(request.getUsername().trim())
                .email(request.getEmail().trim().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .rol(Rol.EMPLEADO)
                .build();

        try {
            usuarioRepository.save(usuario);
        } catch (Exception e) {
            throw new BadRequestException("Error al registrar el empleado: " + e.getMessage());
        }

        Map<String, Object> response = new java.util.HashMap<>();
        response.put("id", usuario.getId());
        response.put("username", usuario.getUsername());
        response.put("email", usuario.getEmail());
        response.put("rol", usuario.getRol().name());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

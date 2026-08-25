package com.logitrack.security;

import com.logitrack.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Collections;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(UsuarioRepository usuarioRepository) {
        return username -> usuarioRepository.findByUsername(username)
                .or(() -> usuarioRepository.findByEmail(username))
                .map(user -> new org.springframework.security.core.userdetails.User(
                        user.getUsername(),
                        user.getPassword(),
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRol().name()))))
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter)
            throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, authException) -> {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "No autorizado");
                })
            )
            .authorizeHttpRequests(auth -> auth
                // Recursos estáticos HTML/JS/CSS y endpoints públicos
                .requestMatchers("/", "/index.html", "/login", "/favicon.ico", "/html/**", "/css/**", "/js/**", "/api/auth/login", "/api/auth/register", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                // Registro de empleados restringido exclusivamente a ADMIN
                .requestMatchers(HttpMethod.POST, "/api/auth/register-empleado").hasRole("ADMIN")
                // Módulo de Auditoría restringido exclusivamente a ADMIN
                .requestMatchers("/api/auditorias/**").hasRole("ADMIN")
                // Bodegas: EMPLEADO y AGENTE solo pueden VER; CREAR y ELIMINAR solo ADMIN
                .requestMatchers(HttpMethod.GET, "/api/bodegas/**").hasAnyRole("ADMIN", "EMPLEADO", "AGENTE")
                .requestMatchers(HttpMethod.POST, "/api/bodegas/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/bodegas/**").hasRole("ADMIN")
                // Productos: EMPLEADO y AGENTE pueden VER y CREAR; solo ADMIN puede ELIMINAR
                .requestMatchers(HttpMethod.GET, "/api/productos/**").hasAnyRole("ADMIN", "EMPLEADO", "AGENTE")
                .requestMatchers(HttpMethod.POST, "/api/productos/**").hasAnyRole("ADMIN", "EMPLEADO", "AGENTE")
                .requestMatchers(HttpMethod.DELETE, "/api/productos/**").hasRole("ADMIN")
                // Órdenes de compra: AGENTE no puede cambiar estado (solo ADMIN)
                .requestMatchers(HttpMethod.PATCH, "/api/ordenes/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/ordenes/**").hasAnyRole("ADMIN", "EMPLEADO", "AGENTE")
                .requestMatchers(HttpMethod.POST, "/api/ordenes/**").hasAnyRole("ADMIN", "EMPLEADO", "AGENTE")
                // Demás API endpoints requieren autenticación
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
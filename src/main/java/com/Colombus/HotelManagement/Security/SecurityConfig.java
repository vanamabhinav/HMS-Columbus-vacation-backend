package com.Colombus.HotelManagement.Security;

import com.Colombus.HotelManagement.Models.User;
import com.Colombus.HotelManagement.Services.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public SecurityConfig(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    public JwtUtil jwtUtil() {
        return new JwtUtil();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return new UserDetailsService() {
            @Override
            public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
                // First check for our hardcoded admin user
                if ("ADMIN1".equals(username)) {
                    return org.springframework.security.core.userdetails.User.withUsername("ADMIN1")
                            .password(passwordEncoder.encode("password"))
                            .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")))
                            .build();
                }
                
                // For all other users, load from the database
                Optional<User> userOpt = userService.getUserByUserName(username);
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    // Ensure role is properly prefixed with ROLE_
                    String role = user.getRole();
                    if (!role.startsWith("ROLE_")) {
                        role = "ROLE_" + role;
                    }
                    
                    return org.springframework.security.core.userdetails.User.withUsername(user.getUserName())
                            .password(user.getPassword()) // Password is already encoded in the database
                            .authorities(Collections.singletonList(new SimpleGrantedAuthority(role)))
                            .build();
                }
                
                throw new UsernameNotFoundException("User not found: " + username);
            }
        };
    }
    
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtUtil jwtUtil, UserDetailsService userDetailsService) {
        return new JwtAuthenticationFilter(jwtUtil, userDetailsService);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/login", "/auth/register", "/auth/validate", "/auth/check-user/**").permitAll()
                        // Allow health check endpoints without authentication
                        .requestMatchers("/api/health", "/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers("/auth/pending-approvals").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/auth/approve-user/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/auth/reject-user/**").hasAuthority("ROLE_ADMIN")
                        // All hotel endpoints require authentication
                        .requestMatchers("/hotels/all").authenticated()
                        .requestMatchers("/hotels/search").authenticated()
                        .requestMatchers(HttpMethod.GET, "/hotels/{id}").authenticated()
                        .requestMatchers("/hotels/search/city").authenticated()
                        .requestMatchers("/hotels/search/state").authenticated()
                        .requestMatchers("/hotels/search/location").authenticated()
                        .requestMatchers("/hotels/preferred").authenticated()
                        // Admin-only endpoints
                        .requestMatchers("/hotels/add").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/hotels/{id}").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/hotels/{id}").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/hotels/upload-csv").hasAuthority("ROLE_ADMIN")
                        .anyRequest().authenticated()
                )
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin"));
        configuration.setExposedHeaders(Arrays.asList("Authorization"));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(List.of(authProvider));
    }
}

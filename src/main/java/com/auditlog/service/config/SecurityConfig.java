package com.auditlog.service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

 @Bean
 PasswordEncoder passwordEncoder() {
  return PasswordEncoderFactories.createDelegatingPasswordEncoder();
 }

 @Bean
 UserDetailsService users(PasswordEncoder passwordEncoder) {

  var writer = User.withUsername("writer")
          .password(passwordEncoder.encode(
                  System.getenv()
                          .getOrDefault(
                                  "AUDIT_WRITER_PASSWORD",
                                  "writer-change-me"
                          )
          ))
          .roles("AUDIT_WRITER")
          .build();

  var reader = User.withUsername("reader")
          .password(passwordEncoder.encode(
                  System.getenv()
                          .getOrDefault(
                                  "AUDIT_READER_PASSWORD",
                                  "reader-change-me"
                          )
          ))
          .roles("AUDIT_READER")
          .build();

  var admin = User.withUsername("admin")
          .password(passwordEncoder.encode(
                  System.getenv()
                          .getOrDefault(
                                  "AUDIT_ADMIN_PASSWORD",
                                  "admin-change-me"
                          )
          ))
          .roles(
                  "AUDIT_ADMIN",
                  "AUDIT_WRITER",
                  "AUDIT_READER",
                  "PRIVACY_ADMIN",
                  "AUDITOR"
          )
          .build();

  return new InMemoryUserDetailsManager(
          writer,
          reader,
          admin
  );
 }

 @Bean
 SecurityFilterChain securityFilterChain(HttpSecurity http)
         throws Exception {

  return http
          .csrf(csrf -> csrf.disable())

          .authorizeHttpRequests(auth -> auth

                  // Swagger
                  .requestMatchers(
                          "/swagger-ui/**",
                          "/swagger-ui.html",
                          "/v3/api-docs/**"
                  )
                  .permitAll()

                  // Health endpoint
                  .requestMatchers(
                          "/actuator/health"
                  )
                  .permitAll()

                  // Create audit event
                  .requestMatchers(
                          HttpMethod.POST,
                          "/api/v1/audit-events"
                  )
                  .hasRole("AUDIT_WRITER")

                  // Redaction
                  .requestMatchers(
                          HttpMethod.POST,
                          "/api/v1/audit-events/*/redactions"
                  )
                  .hasRole("PRIVACY_ADMIN")

                  // Archive
                  .requestMatchers(
                          HttpMethod.POST,
                          "/api/v1/audit-events/archive"
                  )
                  .hasRole("AUDIT_ADMIN")

                  // Export
                  .requestMatchers(
                          "/api/v1/audit-events/export/**"
                  )
                  .hasAnyRole(
                          "AUDITOR",
                          "AUDIT_ADMIN"
                  )

                  // Other audit operations
                  .anyRequest()
                  .hasAnyRole(
                          "AUDIT_READER",
                          "AUDITOR",
                          "AUDIT_ADMIN"
                  )
          )

          .httpBasic(Customizer.withDefaults())

          .build();
 }
}
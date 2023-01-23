package uk.gov.justice.probation.courtcasematcher.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@Profile("!unsecured")
public class OAuth2SecurityConfig {

    @Value("${hmpps.sqs.queueAdminRole}")
    private String role;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and().oauth2Client()
            .and()
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/actuator/health", "/actuator/health/ping", "/queue-admin/retry-all-dlqs").permitAll()
                .anyRequest().hasRole(role))
            .oauth2ResourceServer().jwt().jwtAuthenticationConverter(new AuthAwareTokenConverter());
        http.anonymous();
        return http.build();
    }

}

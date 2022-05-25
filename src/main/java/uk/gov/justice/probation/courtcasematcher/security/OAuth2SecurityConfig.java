package uk.gov.justice.probation.courtcasematcher.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;

@EnableWebSecurity
@Profile("!unsecured")
public class OAuth2SecurityConfig extends WebSecurityConfigurerAdapter {

    @Value("${hmpps.sqs.queueAdminRole}")
    private String role;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                // Can't have CSRF protection as requires session
                .csrf().disable()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)

                .and().oauth2Client()


                .and().authorizeRequests(auth ->
                        auth
                                .mvcMatchers(
                                        "/actuator/health",
                                        "/actuator/health/ping"
                                ).permitAll()
                                .anyRequest()
                                .hasRole(role)
                ).oauth2ResourceServer().jwt().jwtAuthenticationConverter(new AuthAwareTokenConverter());
    }
}

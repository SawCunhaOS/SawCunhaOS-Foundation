package br.com.sawcunhaos.foundation.security.configuration;

import br.com.sawcunhaos.foundation.security.exception.AccessDeniedExceptionHandler;
import br.com.sawcunhaos.foundation.security.exception.ExceptionHandlerFilter;
import br.com.sawcunhaos.foundation.security.filter.AuthorizationRequiredFilter;
import br.com.sawcunhaos.foundation.security.filter.ScosCorsFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.session.SessionManagementFilter;

@AutoConfiguration
@RequiredArgsConstructor
public class InsideHttpSecurityConfiguration {

    private final ScosCorsFilter corsFilter;
    private final AuthorizationRequiredFilter authorizationRequiredFilter;
    private final AccessDeniedExceptionHandler accessDeniedExceptionHandler;
    private final ExceptionHandlerFilter exceptionHandlerFilter;

    @Primary
    @Bean("InsideHttpSecurityConfiguration")
    public HttpSecurity insideHttpSecurityConfiguration(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/swagger-ui/**", "/v*/api-docs/**", "/actuator/**").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptionHandling ->
                        exceptionHandling.accessDeniedHandler(accessDeniedExceptionHandler)
                )
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sessionManagement -> sessionManagement.sessionCreationPolicy(SessionCreationPolicy.NEVER))
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))

                .addFilterBefore(exceptionHandlerFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(authorizationRequiredFilter, SessionManagementFilter.class)
                .addFilterBefore(corsFilter,SessionManagementFilter.class);
        return httpSecurity;
    }

}

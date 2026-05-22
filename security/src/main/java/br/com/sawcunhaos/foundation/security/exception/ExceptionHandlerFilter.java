package br.com.sawcunhaos.foundation.security.exception;


import br.com.sawcunhaos.foundation.exception.error.ScosSecurityException;
import br.com.sawcunhaos.foundation.exception.model.ExceptionResponse;
import br.com.sawcunhaos.foundation.security.utils.AuthenticationUtils;
import br.com.sawcunhaos.foundation.utils.specification.LocaleService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Component
@Slf4j
@RequiredArgsConstructor
public class ExceptionHandlerFilter extends OncePerRequestFilter {

    private final LocaleService localeService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } catch (ScosSecurityException e) {
            log.error("InsideSoftwaresException: {}", e.getMessage());
            createResponse(
                    response,
                    e.getCode(),
                    e.getArgs()
            );
        }
    }

    private void createResponse(@NonNull HttpServletResponse response, String code, Object... args) throws IOException {
        ExceptionResponse exceptionResponse = ExceptionResponse.builder()
                .codeError(code)
                .message(localeService.getMessage(code, args))
                .build();

        String body = objectMapper.writeValueAsString(
                AuthenticationUtils.createResponse(exceptionResponse)
        );

        AuthenticationUtils.createResponseHttpServlet(response, body);
    }
}

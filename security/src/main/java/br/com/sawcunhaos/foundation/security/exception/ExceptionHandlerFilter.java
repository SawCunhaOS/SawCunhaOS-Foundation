package br.com.sawcunhaos.foundation.security.exception;


import br.com.sawcunhaos.foundation.exception.error.ScosSecurityException;
import br.com.sawcunhaos.foundation.exception.model.ScosProblemDetails;
import br.com.sawcunhaos.foundation.security.utils.AuthenticationUtils;
import br.com.sawcunhaos.foundation.utils.specification.LocaleService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Catches {@link ScosSecurityException} thrown deeper in the filter chain and
 * renders it as an RFC 9457 {@link ProblemDetail} (HTTP 401), consistent with
 * the rest of the foundation error handling.
 *
 * <p>This filter runs outside the {@code DispatcherServlet}, so it serializes
 * the {@code ProblemDetail} manually via {@link ObjectMapper} and sets the
 * {@code application/problem+json} content type itself.</p>
 */
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
            log.error("ScosSecurityException: {}", e.getMessage());
            writeProblem(request, response, e);
        }
    }

    private void writeProblem(HttpServletRequest request, HttpServletResponse response,
                              ScosSecurityException e) throws IOException {
        ProblemDetail problem = ScosProblemDetails.enrich(
                ScosProblemDetails.of(
                        HttpStatus.UNAUTHORIZED,
                        e.getCode(),
                        "Unauthorized",
                        localeService.getMessage(e.getCode(), e.getArgs()),
                        request.getRequestURI()
                )
        );

        AuthenticationUtils.writeProblemDetail(response, HttpStatus.UNAUTHORIZED.value(), problem, objectMapper);
    }
}

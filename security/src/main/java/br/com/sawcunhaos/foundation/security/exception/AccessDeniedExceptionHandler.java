package br.com.sawcunhaos.foundation.security.exception;

import br.com.sawcunhaos.foundation.exception.model.ScosProblemDetails;
import br.com.sawcunhaos.foundation.security.utils.AuthenticationUtils;
import br.com.sawcunhaos.foundation.utils.enums.ScosExceptionCode;
import br.com.sawcunhaos.foundation.utils.specification.LocaleService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Renders Spring Security {@link AccessDeniedException} (HTTP 403) as an RFC 9457
 * {@link ProblemDetail}, matching the format produced by
 * {@code ExceptionsHandler} so every error path is consistent.
 */
@Component
@RequiredArgsConstructor
public class AccessDeniedExceptionHandler implements AccessDeniedHandler {

    private final LocaleService localeService;
    private final ObjectMapper objectMapper;

    @Override
    public void handle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                       @NonNull AccessDeniedException ex) throws IOException {

        ProblemDetail problem = ScosProblemDetails.enrich(
                ScosProblemDetails.of(
                        HttpStatus.FORBIDDEN,
                        ScosExceptionCode.ACCESS_DENIED,
                        localeService.getMessage(ScosExceptionCode.ACCESS_DENIED.getCode()),
                        request.getRequestURI()
                )
        );

        AuthenticationUtils.writeProblemDetail(response, HttpStatus.FORBIDDEN.value(), problem, objectMapper);
    }
}

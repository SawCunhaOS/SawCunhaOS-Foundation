package br.com.sawcunhaos.foundation.security.exception;

import br.com.sawcunhaos.foundation.exception.model.ExceptionResponse;
import br.com.sawcunhaos.foundation.security.utils.AuthenticationUtils;
import br.com.sawcunhaos.foundation.utils.enums.ScosExceptionCode;
import br.com.sawcunhaos.foundation.utils.specification.LocaleService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class AccessDeniedExceptionHandler implements AccessDeniedHandler {

    private final LocaleService localeService;
    private final ObjectMapper objectMapper;

    @Override
    public void handle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                       @NonNull AccessDeniedException ex) throws IOException {

        ExceptionResponse exceptionResponse = ExceptionResponse.builder()
                .codeError(ScosExceptionCode.ACCESS_DENIED.getCode())
                .message(localeService.getMessage(ScosExceptionCode.ACCESS_DENIED.getCode()))
                .build();

        String body = objectMapper.writeValueAsString(
                AuthenticationUtils.createResponse(exceptionResponse)
        );

        AuthenticationUtils.createResponseHttpServlet(response, body);

    }
}

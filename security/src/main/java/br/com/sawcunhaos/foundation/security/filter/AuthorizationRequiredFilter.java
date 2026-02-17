package br.com.sawcunhaos.foundation.security.filter;

import br.com.sawcunhaos.foundation.exception.model.ExceptionResponse;
import br.com.sawcunhaos.foundation.security.utils.AuthenticationUtils;
import br.com.sawcunhaos.foundation.security.utils.SecurityExceptionCode;
import br.com.sawcunhaos.foundation.utils.specification.LocaleService;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingResponseWrapper;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Component
@Order(99)
@RequiredArgsConstructor
@Log4j2
public class AuthorizationRequiredFilter implements Filter {

    private final LocaleService localeService;
    private final ObjectMapper objectMapper;
    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {

        final HttpServletRequest req = (HttpServletRequest) servletRequest;
        if(req.getRequestURI().contains(contextPath+"/api")) {
            HttpServletResponse res = (HttpServletResponse) servletResponse;
            checkAuthorization(req, res);
        }

        filterChain.doFilter(servletRequest, servletResponse);

    }

    private void checkAuthorization(HttpServletRequest request, HttpServletResponse response) throws IOException {

        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader == null || authorizationHeader.isEmpty()) {
            ExceptionResponse exceptionResponse = ExceptionResponse.builder()
                    .codeError(SecurityExceptionCode.AUTH_005.getCode())
                    .message(localeService.getMessage(SecurityExceptionCode.AUTH_005.getCode()))
                    .build();

            String body = objectMapper.writeValueAsString(
                    AuthenticationUtils.createResponse(exceptionResponse)
            );

            AuthenticationUtils.createResponseHttpServlet(response, body);
            ContentCachingResponseWrapper servletResponse = new ContentCachingResponseWrapper(response);
            servletResponse.copyBodyToResponse();
        }
    }
}

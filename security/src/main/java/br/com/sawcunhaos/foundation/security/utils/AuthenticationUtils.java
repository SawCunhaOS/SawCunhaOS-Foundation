package br.com.sawcunhaos.foundation.security.utils;

import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AuthenticationUtils {

    public static final String HEADER_STRING = "Authorization";

    /** RFC 9457 media type for problem responses. */
    public static final String PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON_VALUE;

    public static String getTokenAuthorization(final HttpServletRequest request){
        return request.getHeader(HEADER_STRING);
    }

    /**
     * Serializes an RFC 9457 {@link ProblemDetail} to the servlet response with
     * the proper status and {@code application/problem+json} content type.
     *
     * <p>Security components run outside the {@code DispatcherServlet}, so they
     * cannot rely on Spring's message converters and must serialize manually.</p>
     */
    public static void writeProblemDetail(HttpServletResponse response, int status,
                                          ProblemDetail problem, ObjectMapper objectMapper) throws IOException {
        response.setStatus(status);
        response.setContentType(PROBLEM_JSON);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(problem));
    }

    public static String getUserNameAuthenticated(){
        return Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getName();
    }
}

package br.com.sawcunhaos.foundation.security.utils;

import br.com.sawcunhaos.foundation.exception.model.ExceptionResponse;
import br.com.sawcunhaos.foundation.utils.dto.response.ScosResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.Objects;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AuthenticationUtils {

    public static final String HEADER_STRING = "Authorization";

    public static String getTokenAuthorization(final HttpServletRequest request){
        return request.getHeader(HEADER_STRING);
    }

    public static ScosResponseDTO<ExceptionResponse> createResponse(ExceptionResponse exceptionResponse){
        return ScosResponseDTO.<ExceptionResponse>builder()
                .data(exceptionResponse)
                .build();
    }

    public static void createResponseHttpServlet(HttpServletResponse response, String body) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(body);
    }

    public static String getUserNameAuthenticated(){
        return Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getName();
    }
}

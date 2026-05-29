package br.com.sawcunhaos.foundation.security.exception;

import br.com.sawcunhaos.foundation.utils.specification.LocaleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link AccessDeniedExceptionHandler}: Spring Security's
 * {@link AccessDeniedException} renders as a 403 RFC 9457 {@link ProblemDetail}
 * with {@code Content-Type: application/problem+json}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AccessDeniedExceptionHandlerTest {

    @Mock LocaleService localeService;
    @Mock ObjectMapper objectMapper;
    @InjectMocks AccessDeniedExceptionHandler handler;

    @Test
    @DisplayName("AccessDeniedException -> 403 problem+json body")
    void rendersAccessDeniedAsProblemDetail() throws Exception {
        when(localeService.getMessage(anyString(), any(Object[].class))).thenReturn("Acesso negado.");
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"status\":403}");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/users");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(request, response, new AccessDeniedException("denied"));

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentType().contains("application/problem+json"));

        ArgumentCaptor<ProblemDetail> captor = ArgumentCaptor.forClass(ProblemDetail.class);
        verify(objectMapper).writeValueAsString(captor.capture());
        ProblemDetail problem = captor.getValue();
        assertEquals(403, problem.getStatus());
        assertEquals("Access Denied", problem.getTitle());
        assertEquals("SCOS-004", problem.getProperties().get("code"));
    }
}

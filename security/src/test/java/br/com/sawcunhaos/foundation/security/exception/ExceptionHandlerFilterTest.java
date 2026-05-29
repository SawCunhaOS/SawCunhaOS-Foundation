package br.com.sawcunhaos.foundation.security.exception;

import br.com.sawcunhaos.foundation.exception.error.ScosSecurityException;
import br.com.sawcunhaos.foundation.security.utils.SecurityExceptionCode;
import br.com.sawcunhaos.foundation.utils.specification.LocaleService;
import jakarta.servlet.FilterChain;
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
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link ExceptionHandlerFilter}: a {@link ScosSecurityException}
 * thrown in the chain is rendered as an RFC 9457 {@link ProblemDetail} (HTTP 401)
 * with {@code Content-Type: application/problem+json}. No Spring context.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExceptionHandlerFilterTest {

    @Mock LocaleService localeService;
    @Mock ObjectMapper objectMapper;
    @InjectMocks ExceptionHandlerFilter filter;

    @Test
    @DisplayName("ScosSecurityException -> 401 problem+json body")
    void rendersSecurityExceptionAsProblemDetail() throws Exception {
        when(localeService.getMessage(anyString(), any(Object[].class))).thenReturn("Não autorizado.");
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"status\":401}");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/secure");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> {
            throw new ScosSecurityException(SecurityExceptionCode.AUTH_005);
        };

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentType().contains("application/problem+json"));

        ArgumentCaptor<ProblemDetail> captor = ArgumentCaptor.forClass(ProblemDetail.class);
        verify(objectMapper).writeValueAsString(captor.capture());
        ProblemDetail problem = captor.getValue();
        assertEquals(401, problem.getStatus());
        assertEquals("Unauthorized", problem.getTitle());
        assertEquals("AUTH-005", problem.getProperties().get("code"));
        assertEquals("/api/secure", problem.getInstance().toString());
    }
}

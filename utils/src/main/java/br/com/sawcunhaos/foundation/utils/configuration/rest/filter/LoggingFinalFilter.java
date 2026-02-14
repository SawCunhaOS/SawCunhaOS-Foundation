package br.com.sawcunhaos.foundation.utils.configuration.rest.filter;


import br.com.sawcunhaos.foundation.utils.configuration.rest.filter.properties.ScosFilterProperties;
import br.com.sawcunhaos.foundation.utils.lgpd.SanitizationBodyComponent;
import br.com.sawcunhaos.foundation.utils.utils.DateUtils;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;

@Slf4j
@Configuration
@Order(100)
@RequiredArgsConstructor
public class LoggingFinalFilter implements Filter {

	private final ScosFilterProperties scosFilterProperties;
	private final SanitizationBodyComponent sanitizationBodyComponent;

	@Override
	public void doFilter(
			ServletRequest request,
			ServletResponse response,
			FilterChain chain
	) throws IOException, ServletException {

		HttpServletResponse res = (HttpServletResponse) response;
		final HttpServletRequest req = (HttpServletRequest) request;
		ContentCachingResponseWrapper servletResponse = new ContentCachingResponseWrapper(res);

		chain.doFilter(request, servletResponse);
		if(req.getRequestURI().contains(scosFilterProperties.getURI())) {
			String responseBody = getResponseBody(servletResponse);
            MDC.put("Response-Time", DateUtils.returnDateCurrent());
            MDC.put("Response-Code", servletResponse.getStatus() + "");
            MDC.put("Response-Content-Type", servletResponse.getContentType());
            MDC.put("Response-Body", responseBody);
            log.info("Final API Call");
            MDC.clear();
		}
		servletResponse.copyBodyToResponse();
	}

	private String getResponseBody(ContentCachingResponseWrapper servletResponse) {
		String responseBody = "Body view not enabled";
		if(scosFilterProperties.isShowResponseBody()) {
			responseBody = sanitizationBodyComponent.sanitizeBody(servletResponse.getContentInputStream());
		}
		return responseBody;
	}
}

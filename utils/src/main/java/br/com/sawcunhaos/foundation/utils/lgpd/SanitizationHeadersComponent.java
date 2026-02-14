package br.com.sawcunhaos.foundation.utils.lgpd;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class SanitizationHeadersComponent {

    private final DataMaskingService dataMaskingService;

    public String sanitizeHeader(final HttpHeaders headers) {
        StringBuilder headerFormatted = new StringBuilder();
        headers.headerNames().forEach(headerName -> {
            String headerValue = dataMaskingService.applyDataMaskValueHeader(headerName, headers.get(headerName).get(0));
            headerFormatted.append("""
                    Header Name -> %s -- %s
                    """.formatted(headerName, headerValue));
        });

        return headerFormatted.toString();
    }

}

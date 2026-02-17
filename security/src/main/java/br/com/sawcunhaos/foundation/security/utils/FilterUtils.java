package br.com.sawcunhaos.foundation.security.utils;

import br.com.sawcunhaos.foundation.security.configuration.properties.CorsProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@Log4j2
public class FilterUtils {

    private final CorsProperties corsProperties;

    public void setLocale(HttpServletRequest request){
        String language = request.getHeader("Accept-Language");
        Locale locale = Objects.nonNull(language) ? Locale.forLanguageTag(language) : Locale.forLanguageTag("pt-BR");
        LocaleContextHolder.setDefaultLocale(locale);
    }

    public void enableCors(HttpServletResponse response){
        response.setHeader("Access-Control-Allow-Origin", corsProperties.getAllowOrigin());
        response.setHeader("Access-Control-Allow-Methods", corsProperties.getAllowMethods());
        response.setHeader("Access-Control-Allow-Headers", corsProperties.getAllowHeaders());
        response.setHeader("Access-Control-Allow-Credentials", corsProperties.getAllowCredentials());
        response.setHeader("Access-Control-Max-Age", corsProperties.getMaxAge());
    }

}

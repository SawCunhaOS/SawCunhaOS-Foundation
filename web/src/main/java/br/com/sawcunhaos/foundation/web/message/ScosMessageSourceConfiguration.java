package br.com.sawcunhaos.foundation.web.message;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.core.Ordered;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Aggregates every {@code scos_message/*.properties} bundle found on the classpath
 * (one per Foundation module that ships translations) into a single {@link MessageSource},
 * instead of the single fixed {@code messages} basename Spring Boot resolves by default.
 * Runs at {@link Ordered#HIGHEST_PRECEDENCE}, same group as
 * {@link MessageSourceAutoConfiguration}, and explicitly before it, so this bean claims the
 * {@code messageSource} name first and Spring Boot's own auto-configuration backs off via its
 * {@code @ConditionalOnMissingBean} instead of racing for the bean name.
 */
@AutoConfiguration(before = MessageSourceAutoConfiguration.class)
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class ScosMessageSourceConfiguration {

    private static final String SCAN_PATTERN = "classpath*:scos_message/*.properties";
    private static final String LOCALE_SUFFIX_PATTERN = "(_[a-z]{2}(_[A-Z]{2})?)?\\.properties$";

    @Bean
    public MessageSource messageSource() throws IOException {

        var resolver = new PathMatchingResourcePatternResolver();

        Resource[] resources = resolver.getResources(SCAN_PATTERN);

        // Dedup by bundle family (strip locale suffix) so e.g. "foo.properties" and
        // "foo_en.properties" resolve to one "foo" basename, not two - passing the
        // locale-suffixed file itself as a basename would make its content leak in
        // as a fallback default for every other locale.
        Set<String> basenames = new LinkedHashSet<>();
        for (Resource resource : resources) {
            String family = resource.getFilename().replaceFirst(LOCALE_SUFFIX_PATTERN, "");
            basenames.add("classpath:scos_message/" + family);
        }

        var messageSource = new ReloadableResourceBundleMessageSource();

        messageSource.setBasenames(basenames.toArray(new String[0]));
        messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());
        messageSource.setFallbackToSystemLocale(false);
        messageSource.setCacheSeconds(-1);
        messageSource.setUseCodeAsDefaultMessage(false);

        return messageSource;
    }

    @PostConstruct
    public void postConstruct() {
        log.info("Scos MessageSource configurado");
    }
}

package br.com.sawcunhaos.foundation.utils.annotation.request;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.core.annotation.AliasFor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ScosRequestMapping(
        method = RequestMethod.POST,
        consumes = MediaType.APPLICATION_JSON_VALUE
)
@CacheEvict(
        allEntries=true
)
public @interface ScosRequestPOST {

    @AliasFor(annotation = RequestMapping.class, attribute = "value")
    String[] uri();

    @AliasFor(annotation = ResponseStatus.class, attribute = "code")
    HttpStatus httpCode();

    @AliasFor(annotation = CacheEvict.class, attribute = "value")
    String[] nameCache() default "";

}

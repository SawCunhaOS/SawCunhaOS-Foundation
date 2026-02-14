package br.com.sawcunhaos.foundation.utils.annotation.request;

import org.springframework.core.annotation.AliasFor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@RequestMapping
@ResponseStatus
@interface ScosRequestMapping {

    @AliasFor(annotation = RequestMapping.class, attribute = "method")
    RequestMethod[] method();

    @AliasFor(annotation = RequestMapping.class, attribute = "value")
    String[] uri() default "";

    @AliasFor(annotation = ResponseStatus.class, attribute = "code")
    HttpStatus httpCode() default HttpStatus.OK;

    @AliasFor(annotation = RequestMapping.class, attribute = "consumes")
    String[] consumes() default {};

}

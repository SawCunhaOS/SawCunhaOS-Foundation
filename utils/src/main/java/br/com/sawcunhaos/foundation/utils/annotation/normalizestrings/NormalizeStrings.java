package br.com.sawcunhaos.foundation.utils.annotation.normalizestrings;

import br.com.sawcunhaos.foundation.utils.enums.StringTransformRule;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NormalizeStrings {

    StringTransformRule function() default StringTransformRule.UPPER_CASE;

}

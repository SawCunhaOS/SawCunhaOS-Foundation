package br.com.sawcunhaos.foundation.utils.validation.taxIdentifier;

import br.com.sawcunhaos.foundation.utils.validation.taxIdentifier.constraint.CnpjValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = CnpjValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface CNPJ {
    String message() default "IFV-010";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

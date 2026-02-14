package br.com.sawcunhaos.foundation.utils.validation.taxIdentifier;

import br.com.sawcunhaos.foundation.utils.validation.taxIdentifier.constraint.TaxIdentifierValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = TaxIdentifierValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface TaxIdentifier {
    String message() default "IFV-012";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

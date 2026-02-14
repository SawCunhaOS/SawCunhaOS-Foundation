package br.com.sawcunhaos.foundation.utils.validation.taxIdentifier.constraint;

import br.com.caelum.stella.validation.CNPJValidator;
import br.com.caelum.stella.validation.CPFValidator;
import br.com.sawcunhaos.foundation.utils.validation.taxIdentifier.TaxIdentifier;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class TaxIdentifierValidator implements ConstraintValidator<TaxIdentifier, String> {

    private final CNPJValidator cnpjValidator = new CNPJValidator();
    private final CPFValidator cpfValidator = new CPFValidator();

    @Override
    public boolean isValid(String taxIdentifier, ConstraintValidatorContext constraintValidatorContext) {
        return cnpjValidator.invalidMessagesFor(taxIdentifier).isEmpty() || cpfValidator.invalidMessagesFor(taxIdentifier).isEmpty();
    }

}

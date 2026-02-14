package br.com.sawcunhaos.foundation.utils.validation.taxIdentifier.constraint;

import br.com.caelum.stella.validation.CNPJValidator;
import br.com.sawcunhaos.foundation.utils.validation.taxIdentifier.CNPJ;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class CnpjValidator implements ConstraintValidator<CNPJ, String> {

    private final CNPJValidator cnpjValidator = new CNPJValidator();

    @Override
    public boolean isValid(String cnpj, ConstraintValidatorContext constraintValidatorContext) {
        return cnpjValidator.invalidMessagesFor(cnpj).isEmpty();
    }

}

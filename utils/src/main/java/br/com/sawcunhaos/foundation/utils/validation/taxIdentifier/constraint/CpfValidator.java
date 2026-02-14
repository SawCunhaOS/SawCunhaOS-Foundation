package br.com.sawcunhaos.foundation.utils.validation.taxIdentifier.constraint;

import br.com.caelum.stella.validation.CPFValidator;
import br.com.sawcunhaos.foundation.utils.validation.taxIdentifier.CPF;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class CpfValidator implements ConstraintValidator<CPF, String> {

    private final CPFValidator cpfValidator = new CPFValidator();

    @Override
    public boolean isValid(String cpf, ConstraintValidatorContext constraintValidatorContext) {
        return cpfValidator.invalidMessagesFor(cpf).isEmpty();
    }

}

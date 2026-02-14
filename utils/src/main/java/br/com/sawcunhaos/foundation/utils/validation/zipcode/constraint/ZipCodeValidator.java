package br.com.sawcunhaos.foundation.utils.validation.zipcode.constraint;

import br.com.sawcunhaos.foundation.utils.validation.zipcode.ZipCode;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ZipCodeValidator implements ConstraintValidator<ZipCode, String> {

    private static final Pattern CEP_VALID_REGEX = Pattern.compile("^\\d{2}\\d{3}(-)?\\d{3}$");

    public boolean isValid(String zipCode, ConstraintValidatorContext cxt) {
        if(StringUtils.isBlank(zipCode)) {
            return false;
        }
        Matcher matcher = CEP_VALID_REGEX.matcher(zipCode);
        return matcher.find();
    }
}

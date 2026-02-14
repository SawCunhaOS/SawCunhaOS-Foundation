package br.com.sawcunhaos.foundation.utils.validation.zipcode.constraint;

import br.com.sawcunhaos.foundation.utils.validation.zipcode.ZipCode;

public record ZipCodeDTO(
        @ZipCode String zipCode
) {
}

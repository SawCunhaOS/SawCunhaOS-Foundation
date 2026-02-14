package br.com.sawcunhaos.foundation.utils.validation.zipcode.constraint;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZipCodeValidatorTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void mustReturnThatTheZipCodeIsValid() {
        ZipCodeDTO zipCodeDTO = createZipCode("36204665");

        Set<ConstraintViolation<ZipCodeDTO>> violations = validator.validate(zipCodeDTO);

        assertTrue(violations.isEmpty());
    }

    @Test
    void mustReturnThatTheZipCodeIsNotValid() {
        ZipCodeDTO zipCodeDTO = createZipCode("362046659");

        Set<ConstraintViolation<ZipCodeDTO>> violations = validator.validate(zipCodeDTO);

        assertFalse(violations.isEmpty());
        assertEquals("IFV-009", violations.stream().findFirst().get().getMessage());
    }

    private ZipCodeDTO createZipCode(final String zipCode) {
        return new ZipCodeDTO(zipCode);
    }

}

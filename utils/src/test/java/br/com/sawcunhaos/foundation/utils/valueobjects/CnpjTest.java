
/*
 *
 *  * Copyright 2026 SawCunha Open System - SawCunhaOS-Foundation
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *
 */

package br.com.sawcunhaos.foundation.utils.valueobjects;

import br.com.sawcunhaos.foundation.utils.enums.ScosExceptionCode;
import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CNPJ Tests")
class CnpjTest {

    @Nested
    @DisplayName("Valid CNPJ scenarios")
    class ValidCnpjScenarios {

        @Test
        @DisplayName("Should create CNPJ with valid number")
        void shouldCreateCnpjWithValidNumber() {
            // Arrange
            String validCnpj = "11222333000181";

            // Act
            Cnpj cnpj = new Cnpj(validCnpj);

            // Assert
            assertNotNull(cnpj);
            assertEquals(validCnpj, cnpj.getCnpj());
            assertEquals("CNPJ", cnpj.getType());
        }

        @ParameterizedTest
        @ValueSource(strings = {"11222333000181", "OUF14CXQIHCK68", "52508598049801"})
        @DisplayName("Should recognize various valid CNPJ numbers")
        void shouldRecognizeVariousValidCnpjs(String validCnpj) {
            // Act
            Cnpj cnpj = new Cnpj(validCnpj);

            // Assert
            assertEquals("CNPJ", cnpj.getType());
            assertEquals(validCnpj, cnpj.getCnpj());
        }

        @Test
        @DisplayName("Should maintain correct type for any valid CNPJ")
        void shouldMaintainCorrectTypeForValidCnpj() {
            // Arrange
            String validCnpj = "11222333000181";
            Cnpj cnpj = new Cnpj(validCnpj);

            // Assert
            assertEquals("CNPJ", cnpj.getType());
        }
    }

    @Nested
    @DisplayName("Invalid numeric scenarios")
    class InvalidNumericScenarios {

        @Test
        @DisplayName("Should throw ScosException for invalid CNPJ with all zeros")
        void shouldThrowExceptionForAllZerosCnpj() {
            // Arrange
            String invalidCnpj = "00000000000000";

            // Act & Assert
            ScosException exception = assertThrows(ScosException.class, () -> new Cnpj(invalidCnpj));
            assertEquals(ScosExceptionCode.CNPJ_INVALID.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("Should throw ScosException for invalid CNPJ with all ones")
        void shouldThrowExceptionForAllOnesCnpj() {
            // Arrange
            String invalidCnpj = "11111111111111";

            // Act & Assert
            ScosException exception = assertThrows(ScosException.class, () -> new Cnpj(invalidCnpj));
            assertEquals(ScosExceptionCode.CNPJ_INVALID.getCode(), exception.getCode());
        }

        @ParameterizedTest
        @ValueSource(strings = {"00000000000000", "11111111111111", "12345678901234", "99999999999999"})
        @DisplayName("Should throw ScosException for various invalid CNPJ formats")
        void shouldThrowExceptionForVariousInvalidCnpjs(String invalidCnpj) {
            // Act & Assert
            assertThrows(ScosException.class, () -> new Cnpj(invalidCnpj));
        }

        @Test
        @DisplayName("Should throw ScosException for empty string")
        void shouldThrowExceptionForEmptyString() {
            // Act & Assert
            assertThrows(ScosException.class, () -> new Cnpj(""));
        }

        @Test
        @DisplayName("Should throw ScosException for CNPJ with wrong number of digits")
        void shouldThrowExceptionForWrongNumberOfDigits() {
            // Act & Assert
            assertThrows(ScosException.class, () -> new Cnpj("112223330001"));
        }

        @Test
        @DisplayName("Should throw ScosException for CPF instead of CNPJ")
        void shouldThrowExceptionForCpf() {
            // Act & Assert
            assertThrows(ScosException.class, () -> new Cnpj("11144477735"));
        }
    }

    @Nested
    @DisplayName("Alphanumeric CNPJ scenarios")
    class AlphanumericCnpjScenarios {

        @Test
        @DisplayName("Should reject CNPJ with alphabetic characters")
        void shouldRejectCnpjWithAlphabeticCharacters() {
            // Act & Assert
            assertThrows(ScosException.class, () -> new Cnpj("11222333A00181"));
        }

        @Test
        @DisplayName("Should reject CNPJ with letters at the beginning")
        void shouldRejectCnpjWithLettersAtBeginning() {
            // Act & Assert
            assertThrows(ScosException.class, () -> new Cnpj("AB222333000181"));
        }

        @Test
        @DisplayName("Should reject CNPJ with letters at the end")
        void shouldRejectCnpjWithLettersAtEnd() {
            // Act & Assert
            assertThrows(ScosException.class, () -> new Cnpj("11222333000AB"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"1A222333000181", "11A22333000181", "112A2333000181", "1122A333000181",
                                "11222A33000181", "112223A3000181", "1122233A000181", "11222333A00181",
                                "112223330A0181", "1122233300A181", "11222333000A81", "112223330001A1",
                                "1122233300018A"})
        @DisplayName("Should reject CNPJ with letters at various positions")
        void shouldRejectCnpjWithLettersAtVariousPositions(String cnpjWithLetter) {
            // Act & Assert
            assertThrows(ScosException.class, () -> new Cnpj(cnpjWithLetter));
        }

        @ParameterizedTest
        @ValueSource(strings = {"ABC22333000181", "ABCD33000181", "ABCDEFGHIJKLMN"})
        @DisplayName("Should reject CNPJ with multiple alphabetic characters")
        void shouldRejectCnpjWithMultipleAlphabeticCharacters(String cnpjWithLetters) {
            // Act & Assert
            assertThrows(ScosException.class, () -> new Cnpj(cnpjWithLetters));
        }

        @Test
        @DisplayName("Should reject CNPJ with special characters")
        void shouldRejectCnpjWithSpecialCharacters() {
            // Act & Assert
            assertThrows(ScosException.class, () -> new Cnpj("11222333!00181"));
        }

        @Test
        @DisplayName("Should reject CNPJ with spaces")
        void shouldRejectCnpjWithSpaces() {
            // Act & Assert
            assertThrows(ScosException.class, () -> new Cnpj("11222333 000181"));
        }

        @Test
        @DisplayName("Should reject formatted CNPJ with dots and slashes")
        void shouldRejectFormattedCnpj() {
            // Act & Assert
            assertThrows(ScosException.class, () -> new Cnpj("11.222.333/0001-81"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"11222333!00181", "11222333#00181", "11222333$00181", "11222333%00181", "11222333&00181"})
        @DisplayName("Should reject CNPJ with various special characters")
        void shouldRejectCnpjWithVariousSpecialCharacters(String cnpjWithSpecialChar) {
            // Act & Assert
            assertThrows(ScosException.class, () -> new Cnpj(cnpjWithSpecialChar));
        }

        @Test
        @DisplayName("Should reject CNPJ with hyphens")
        void shouldRejectCnpjWithHyphens() {
            // Act & Assert
            assertThrows(ScosException.class, () -> new Cnpj("11-222-333-0001-81"));
        }

        @Test
        @DisplayName("Should reject CNPJ with slashes")
        void shouldRejectCnpjWithSlashes() {
            // Act & Assert
            assertThrows(ScosException.class, () -> new Cnpj("11/222/333/0001/81"));
        }

        @Test
        @DisplayName("Should reject mixed alphanumeric CNPJ")
        void shouldRejectMixedAlphanumericCnpj() {
            // Act & Assert
            assertThrows(ScosException.class, () -> new Cnpj("11222A33000B81"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"11222333000181abc", "xyz11222333000181", "11222abc333000181"})
        @DisplayName("Should reject CNPJ with alphanumeric combinations")
        void shouldRejectCnpjWithAlphanumericCombinations(String mixedCnpj) {
            // Act & Assert
            assertThrows(ScosException.class, () -> new Cnpj(mixedCnpj));
        }
    }

    @Nested
    @DisplayName("SetTaxIdentifier method tests")
    class SetTaxIdentifierMethodTests {

        @Test
        @DisplayName("Should update CNPJ with valid new value")
        void shouldUpdateCnpjWithValidValue() {
            // Arrange
            Cnpj cnpj = new Cnpj("11222333000181");
            String newValidCnpj = "OUF14CXQIHCK68";

            // Act
            cnpj.setTaxIdentifier(newValidCnpj);

            // Assert
            assertEquals(newValidCnpj, cnpj.getCnpj());
            assertEquals("CNPJ", cnpj.getType());
        }

        @Test
        @DisplayName("Should throw ScosException when setting invalid CNPJ")
        void shouldThrowExceptionWhenSettingInvalidCnpj() {
            // Arrange
            Cnpj cnpj = new Cnpj("11222333000181");

            // Act & Assert
            assertThrows(ScosException.class, () -> cnpj.setTaxIdentifier("00000000000000"));
        }

        @Test
        @DisplayName("Should throw ScosException when setting alphanumeric CNPJ")
        void shouldThrowExceptionWhenSettingAlphanumericCnpj() {
            // Arrange
            Cnpj cnpj = new Cnpj("11222333000181");

            // Act & Assert
            assertThrows(ScosException.class, () -> cnpj.setTaxIdentifier("11222333A00181"));
        }

        @Test
        @DisplayName("Should maintain CNPJ unchanged when exception occurs during update")
        void shouldMaintainCnpjWhenExceptionOccurs() {
            // Arrange
            String originalCnpj = "11222333000181";
            Cnpj cnpj = new Cnpj(originalCnpj);

            // Act
            assertThrows(ScosException.class, () -> cnpj.setTaxIdentifier("11111111111111"));

            // Assert
            assertEquals(originalCnpj, cnpj.getCnpj());
            assertEquals("CNPJ", cnpj.getType());
        }

        @Test
        @DisplayName("Should allow multiple valid updates")
        void shouldAllowMultipleValidUpdates() {
            // Arrange
            Cnpj cnpj = new Cnpj("11222333000181");
            String newCnpj1 = "OUF14CXQIHCK68";
            String newCnpj2 = "52508598049801";

            // Act
            cnpj.setTaxIdentifier(newCnpj1);
            assertEquals(newCnpj1, cnpj.getCnpj());

            cnpj.setTaxIdentifier(newCnpj2);

            // Assert
            assertEquals(newCnpj2, cnpj.getCnpj());
            assertEquals("CNPJ", cnpj.getType());
        }
    }

    @Nested
    @DisplayName("Getter tests")
    class GetterTests {

        @Test
        @DisplayName("Should return correct CNPJ value")
        void shouldReturnCorrectCnpjValue() {
            // Arrange
            String validCnpj = "11222333000181";
            Cnpj cnpj = new Cnpj(validCnpj);

            // Act & Assert
            assertEquals(validCnpj, cnpj.getCnpj());
        }

        @Test
        @DisplayName("Should return correct type")
        void shouldReturnCorrectType() {
            // Arrange
            Cnpj cnpj = new Cnpj("11222333000181");

            // Act & Assert
            assertEquals("CNPJ", cnpj.getType());
        }

        @Test
        @DisplayName("Should always return CNPJ as type")
        void shouldAlwaysReturnCnpjAsType() {
            // Arrange
            String validCnpj = "OUF14CXQIHCK68";
            Cnpj cnpj = new Cnpj(validCnpj);

            // Act & Assert
            assertEquals("CNPJ", cnpj.getType());
            assertNotEquals("CPF", cnpj.getType());
        }
    }

    @Nested
    @DisplayName("Default constructor tests")
    class DefaultConstructorTests {

        @Test
        @DisplayName("Should allow creation with protected no-arg constructor for JPA")
        void shouldAllowProtectedNoArgConstructor() {
            // This test verifies that JPA can instantiate the class
            assertDoesNotThrow(() -> new Cnpj("11222333000181"));
        }
    }

}

